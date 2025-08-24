import os
import json
import base64
import logging
from datetime import datetime, timedelta
from flask import Flask, request, jsonify, render_template
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import func
import random

from models import db, User, Disposal, DailyDisposalLog, Challenge, Hotspot, UserChallengeProgress
from gemini_service import validate_disposal_with_ai_video
from hotspot_generator import generate_hotspots, generate_offline_hotspots
from offline_manager import OfflineManager

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class Base(DeclarativeBase):
    pass

# Create the app
app = Flask(__name__)
CORS(app)

# Setup secret key
app.secret_key = os.environ.get("FLASK_SECRET_KEY", "trackeco_secret_key_2024")

# Configure the database
app.config["SQLALCHEMY_DATABASE_URI"] = os.environ.get("DATABASE_URL", "postgresql://postgres:password@localhost:5432/trackeco")
app.config["SQLALCHEMY_ENGINE_OPTIONS"] = {
    "pool_recycle": 300,
    "pool_pre_ping": True,
    "pool_timeout": 20,
    "max_overflow": 0,
    "echo": False
}
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

# Initialize the app with the extension
db.init_app(app)

# Initialize offline manager
offline_manager = OfflineManager()

# Predefined challenge pool
CHALLENGE_POOL = [
    {"type": "COUNT", "category": "Plastic", "goal": 5, "reward": 50, "description": "Dispose of 5 Plastic items today"},
    {"type": "COUNT", "category": "Metal", "goal": 3, "reward": 50, "description": "Dispose of 3 Metal items today"},
    {"type": "COUNT", "category": "Glass", "goal": 2, "reward": 50, "description": "Dispose of 2 Glass items today"},
    {"type": "VARIETY", "category": "Metal", "goal": 3, "reward": 50, "description": "Dispose of 3 different Metal sub-types today"},
    {"type": "VARIETY", "category": "Plastic", "goal": 4, "reward": 50, "description": "Dispose of 4 different Plastic sub-types today"},
    {"type": "HOTSPOT", "goal": 1, "reward": 50, "description": "Complete 1 disposal inside a Litter Hotspot"},
    {"type": "COUNT", "category": "Paper/Cardboard", "goal": 3, "reward": 50, "description": "Dispose of 3 Paper/Cardboard items today"},
]

with app.app_context():
    # Import models to ensure tables are created
    import models
    db.create_all()
    
    # Initialize challenges if they don't exist
    if Challenge.query.count() == 0:
        for i, challenge_data in enumerate(CHALLENGE_POOL):
            challenge = Challenge(
                challenge_id=f"challenge_{i+1}",
                challenge_type=challenge_data["type"],
                category=challenge_data.get("category"),
                goal=challenge_data["goal"],
                reward=challenge_data["reward"],
                description=challenge_data["description"]
            )
            db.session.add(challenge)
        db.session.commit()
        logger.info("Initialized challenge pool")

@app.route('/')
def index():
    """Serve the main application"""
    return render_template('index.html')

@app.route('/api/user/<user_id>')
def get_user(user_id):
    """Get user profile data with offline caching"""
    try:
        user = User.query.filter_by(user_id=user_id).first()
        if not user:
            # Create new user
            user = User(user_id=user_id)
            db.session.add(user)
            db.session.commit()
            
        # Get user's daily challenge
        daily_challenge = get_or_assign_daily_challenge(user_id)
        
        # Get user's challenge progress
        progress = UserChallengeProgress.query.filter_by(
            user_id=user_id,
            challenge_id=daily_challenge.challenge_id
        ).first()
        
        current_progress = progress.current_progress if progress else 0
        
        user_data = {
            "user_id": user.user_id,
            "xp": user.xp,
            "points": user.points,
            "streak": user.streak,
            "eco_rank": calculate_eco_rank(user.xp),
            "has_completed_first_disposal": user.has_completed_first_disposal,
            "daily_challenge": {
                "description": daily_challenge.description,
                "progress": current_progress,
                "goal": daily_challenge.goal,
                "reward": daily_challenge.reward
            }
        }
        
        # Cache user data for offline access
        offline_manager.cache_user_data(user_id, user_data)
        
        return jsonify(user_data)
    except Exception as e:
        logger.error(f"Error getting user {user_id}: {str(e)}")
        
        # Fallback to cached data
        try:
            cached_data = offline_manager.get_cached_user_data(user_id)
            if cached_data:
                logger.info(f"Returning cached data for user {user_id}")
                return jsonify(cached_data)
        except Exception as cache_error:
            logger.error(f"Error getting cached user data: {str(cache_error)}")
        
        return jsonify({"error": str(e)}), 500

@app.route('/api/hotspots')
def get_hotspots():
    """Get current litter hotspots with offline fallback"""
    try:
        # Try to get from main database first
        hotspots = Hotspot.query.filter(Hotspot.expires_at > datetime.utcnow()).all()
        hotspot_data = [{
            "id": h.id,
            "latitude": float(h.latitude),
            "longitude": float(h.longitude),
            "intensity": h.intensity,
            "created_at": h.created_at.isoformat()
        } for h in hotspots]
        
        # Cache hotspots for ofafline use
        offline_manager.cache_hotspots(hotspot_data)
        
        return jsonify(hotspot_data)
    except Exception as e:
        logger.error(f"Error getting hotspots from database: {str(e)}")
        
        # Fallback to cached data
        try:
            cached_hotspots = offline_manager.get_cached_hotspots()
            logger.info(f"Returning {len(cached_hotspots)} cached hotspots")
            return jsonify(cached_hotspots)
        except Exception as cache_error:
            logger.error(f"Error getting cached hotspots: {str(cache_error)}")
            return jsonify({"error": str(e)}), 500

# Global error handlers to ensure JSON responses
@app.errorhandler(500)
def internal_error(error):
    logger.error(f"Internal server error: {str(error)}")
    return jsonify({
        "success": False,
        "error": "Internal server error",
        "reason_code": "SERVER_ERROR",
        "message": "Something went wrong. Please try again."
    }), 500

@app.errorhandler(404)
def not_found_error(error):
    return jsonify({
        "success": False,
        "error": "Not found",
        "reason_code": "NOT_FOUND",
        "message": "The requested resource was not found."
    }), 404

@app.route('/api/verify_disposal', methods=['POST'])
def verify_disposal():
    """Verify disposal action with AI and award points (with offline support)"""
    try:
        data = request.get_json()
        user_id = data.get('user_id')
        latitude = data.get('latitude')
        longitude = data.get('longitude')
        video_b64 = data.get('video')  # Base64 encoded video
        
        if not all([user_id, latitude, longitude, video_b64]):
            return jsonify({"error": "Missing required fields"}), 400
            
        # Get user
        user = User.query.filter_by(user_id=user_id).first()
        if not user:
            return jsonify({"error": "User not found"}), 404
            
        # Anti-cheat: Check GPS cooldown/distance
        recent_disposal = Disposal.query.filter_by(user_id=user_id).filter(
            Disposal.timestamp > datetime.utcnow() - timedelta(minutes=1)
        ).order_by(Disposal.timestamp.desc()).first()
        
        if recent_disposal:
            # Simple distance check (approximately 20 meters)
            lat_diff = abs(float(latitude) - float(recent_disposal.latitude))
            lon_diff = abs(float(longitude) - float(recent_disposal.longitude))
            if lat_diff < 0.0002 and lon_diff < 0.0002:  # Roughly 20 meters
                return jsonify({
                    "success": False,
                    "reason_code": "FAIL_TOO_CLOSE",
                    "message": "Please move to a different location before your next disposal."
                }), 400
        
        # Decode video from base64
        try:
            video_bytes = base64.b64decode(video_b64.split(',')[1] if ',' in video_b64 else video_b64)
        except Exception as e:
            logger.error(f"Error decoding video: {str(e)}")
            return jsonify({"error": "Invalid video data"}), 400
            
        # Check if online for AI validation
        if offline_manager.is_online():
            # Call Gemini AI for validation
            ai_result = validate_disposal_with_ai_video(video_bytes)
            
            if not ai_result["success"]:
                return jsonify({
                    "success": False,
                    "reason_code": ai_result["reason_code"],
                    "message": get_failure_message(ai_result["reason_code"])
                })
        else:
            # Offline mode - cache disposal for later processing
            success = offline_manager.cache_disposal_offline(
                user_id, float(latitude), float(longitude), [video_b64]
            )
            
            if success:
                return jsonify({
                    "success": True,
                    "points_earned": 10,  # Standard offline points
                    "xp_earned": 15,
                    "waste_category": "General Waste",  # Placeholder
                    "waste_sub_type": "Other General Waste",  # Placeholder
                    "bonuses_awarded": [],
                    "challenges_completed": [],
                    "reason_code": "OFFLINE_CACHED",
                    "message": "Disposal cached offline and will be validated when connection is restored.",
                    "offline_mode": True
                })
            else:
                return jsonify({
                    "success": False,
                    "reason_code": "OFFLINE_ERROR",
                    "message": "Unable to cache disposal offline. Please try again."
                }), 500
            
        # AI validation successful - calculate rewards
        waste_category = ai_result["waste_category"]
        waste_sub_type = ai_result["waste_sub_type"]
        
        # Check if this is first disposal
        points_earned = 10  # Standard reward
        xp_earned = 15     # Standard XP
        bonuses_awarded = []
        challenges_completed = []
        
        if not user.has_completed_first_disposal:
            points_earned = 200  # First-ever bonus
            xp_earned = 50
            user.has_completed_first_disposal = True
            bonuses_awarded.append("First Disposal Bonus: +190 points!")
            
        # Discovery bonus for new category
        if not has_discovered_category(user_id, waste_category):
            xp_earned += 10
            bonuses_awarded.append(f"Discovery Bonus: +10 XP for first {waste_category}!")
            
        # Check daily challenge completion
        daily_challenge = get_or_assign_daily_challenge(user_id)
        challenge_bonus = check_daily_challenge_completion(user_id, daily_challenge, waste_category, waste_sub_type, latitude, longitude)
        
        if challenge_bonus > 0:
            points_earned += challenge_bonus
            challenges_completed.append(f"Daily Challenge Complete! +{challenge_bonus} points")
            
        # Update user stats
        user.points += points_earned
        user.xp += xp_earned
        user.last_disposal_date = datetime.utcnow().date()
        
        # Update streak
        yesterday = datetime.utcnow().date() - timedelta(days=1)
        if user.last_disposal_date == yesterday:
            user.streak += 1
        elif user.last_disposal_date != datetime.utcnow().date():
            user.streak = 1
            
        # Log the disposal with database error handling
        try:
            disposal = Disposal(
                user_id=user_id,
                latitude=latitude,
                longitude=longitude,
                waste_category=waste_category,
                waste_sub_type=waste_sub_type,
                points_awarded=points_earned
            )
            db.session.add(disposal)
            
            # Log for anti-cheat (daily sub-type tracking)
            daily_log = DailyDisposalLog(
                user_id=user_id,
                date=datetime.utcnow().date(),
                waste_sub_type=waste_sub_type
            )
            db.session.add(daily_log)
            
            db.session.commit()
        
        except Exception as db_error:
            logger.error(f"Database error in verify_disposal: {str(db_error)}")
            # Roll back the transaction
            try:
                db.session.rollback()
            except:
                pass
            # Still return success to user since AI validation passed
        
        return jsonify({
            "success": True,
            "points_earned": points_earned,
            "xp_earned": xp_earned,
            "waste_category": waste_category,
            "waste_sub_type": waste_sub_type,
            "bonuses_awarded": bonuses_awarded,
            "challenges_completed": challenges_completed,
            "reason_code": "SUCCESS",
            "new_total_points": user.points,
            "new_total_xp": user.xp,
            "new_streak": user.streak,
            "eco_rank": calculate_eco_rank(user.xp)
        })
        
    except Exception as e:
        logger.error(f"Error verifying disposal: {str(e)}")
        # Ensure we rollback any uncommitted transactions
        try:
            db.session.rollback()
        except:
            pass
        return jsonify({
            "success": False,
            "reason_code": "SERVER_ERROR", 
            "message": "An error occurred processing your disposal. Please try again."
        }), 500

@app.route('/api/generate_hotspots', methods=['POST'])
def trigger_hotspot_generation():
    """Manually trigger hotspot generation (normally runs daily)"""
    try:
        generate_hotspots()
        return jsonify({"message": "Hotspots generated successfully"})
    except Exception as e:
        logger.error(f"Error generating hotspots: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/sync_offline', methods=['POST'])
def sync_offline_data():
    """Sync offline cached data with server"""
    try:
        sync_stats = offline_manager.sync_offline_data(app.app_context())
        return jsonify({
            "message": "Offline data synced successfully",
            "synced_disposals": sync_stats['synced_disposals'],
            "failed_disposals": sync_stats['failed_disposals']
        })
    except Exception as e:
        logger.error(f"Error syncing offline data: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/offline_status')
def get_offline_status():
    """Get offline cache status"""
    try:
        pending_disposals = offline_manager.get_pending_disposals()
        return jsonify({
            "is_online": offline_manager.is_online(),
            "pending_disposals": len(pending_disposals),
            "cache_available": True
        })
    except Exception as e:
        logger.error(f"Error getting offline status: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/user/<user_id>/discovered_categories')
def get_discovered_categories(user_id):
    """Get categories discovered by user"""
    try:
        categories = [
            "Plastic", "Paper/Cardboard", "Glass", "Metal", 
            "Organic", "E-Waste", "General Waste"
        ]
        
        discovered = {}
        for category in categories:
            discovered[category] = has_discovered_category(user_id, category)
        
        return jsonify(discovered)
    except Exception as e:
        logger.error(f"Error getting discovered categories for user {user_id}: {str(e)}")
        return jsonify({"error": str(e)}), 500

def get_or_assign_daily_challenge(user_id):
    """Get or assign daily challenge for user"""
    today = datetime.utcnow().date()
    
    # Check if user has progress for today
    progress = UserChallengeProgress.query.filter_by(user_id=user_id).filter(
        func.date(UserChallengeProgress.assigned_date) == today
    ).first()
    
    if progress:
        return Challenge.query.filter_by(challenge_id=progress.challenge_id).first()
    
    # Assign random challenge
    challenges = Challenge.query.all()
    if not challenges:
        return None
        
    selected_challenge = random.choice(challenges)
    
    # Create progress record
    new_progress = UserChallengeProgress(
        user_id=user_id,
        challenge_id=selected_challenge.challenge_id,
        current_progress=0,
        assigned_date=datetime.utcnow()
    )
    db.session.add(new_progress)
    db.session.commit()
    
    return selected_challenge

def check_daily_challenge_completion(user_id, challenge, waste_category, waste_sub_type, latitude, longitude):
    """Check if disposal completes daily challenge"""
    if not challenge:
        return 0
        
    progress = UserChallengeProgress.query.filter_by(
        user_id=user_id,
        challenge_id=challenge.challenge_id
    ).filter(
        func.date(UserChallengeProgress.assigned_date) == datetime.utcnow().date()
    ).first()
    
    if not progress or progress.is_completed:
        return 0
        
    if challenge.challenge_type == "COUNT":
        if challenge.category == waste_category:
            progress.current_progress += 1
            if progress.current_progress >= challenge.goal:
                progress.is_completed = True
                db.session.commit()
                return challenge.reward
                
    elif challenge.challenge_type == "VARIETY":
        if challenge.category == waste_category:
            # Check if this sub-type was already logged today
            today = datetime.utcnow().date()
            existing = DailyDisposalLog.query.filter_by(
                user_id=user_id,
                date=today,
                waste_sub_type=waste_sub_type
            ).first()
            
            if not existing:  # New sub-type for today
                progress.current_progress += 1
                if progress.current_progress >= challenge.goal:
                    progress.is_completed = True
                    db.session.commit()
                    return challenge.reward
                    
    elif challenge.challenge_type == "HOTSPOT":
        # Check if location is in a hotspot
        hotspots = Hotspot.query.filter(Hotspot.expires_at > datetime.utcnow()).all()
        for hotspot in hotspots:
            # Simple distance check for hotspot
            lat_diff = abs(float(latitude) - float(hotspot.latitude))
            lon_diff = abs(float(longitude) - float(hotspot.longitude))
            if lat_diff < 0.001 and lon_diff < 0.001:  # Within hotspot
                progress.current_progress += 1
                if progress.current_progress >= challenge.goal:
                    progress.is_completed = True
                    db.session.commit()
                    return challenge.reward
                break
    
    db.session.commit()
    return 0

def has_discovered_category(user_id, category):
    """Check if user has discovered this category before"""
    return Disposal.query.filter_by(user_id=user_id, waste_category=category).first() is not None

def calculate_eco_rank(xp):
    """Calculate eco rank based on XP"""
    if xp < 100:
        return "Eco Novice"
    elif xp < 300:
        return "Eco Cadet"
    elif xp < 600:
        return "Eco Guardian"
    elif xp < 1000:
        return "Eco Champion"
    elif xp < 1500:
        return "Eco Master"
    else:
        return "Eco Legend"

def get_failure_message(reason_code):
    """Get user-friendly failure message"""
    messages = {
        "FAIL_LITTERING": "The waste was not disposed of in a proper receptacle. Please use a trash bin or recycling container.",
        "FAIL_WASTE_USABLE": "The item appears to be new or usable. Please only dispose of actual waste items.",
        "FAIL_OBJECT_TOO_SMALL": "The item is too small to be meaningful for our cleanup goals.",
        "FAIL_UNCLEAR": "The disposal action was unclear. Please ensure good lighting and clear visibility of the waste disposal."
    }
    return messages.get(reason_code, "Disposal validation failed. Please try again with a clearer recording.")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
