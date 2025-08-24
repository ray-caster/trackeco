from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, Date, Text
from datetime import datetime

class Base(DeclarativeBase):
    pass

db = SQLAlchemy(model_class=Base)

class User(db.Model):
    __tablename__ = 'users'
    
    id = Column(Integer, primary_key=True)
    user_id = Column(String(255), unique=True, nullable=False)
    xp = Column(Integer, default=0)
    points = Column(Integer, default=0)
    rupiah_balance = Column(Float, default=0.0)
    streak = Column(Integer, default=0)
    last_disposal_date = Column(Date)
    last_rupiah_bonus_date = Column(Date)
    has_completed_first_disposal = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    def __repr__(self):
        return f'<User {self.user_id}>'

class Disposal(db.Model):
    __tablename__ = 'disposals'
    
    id = Column(Integer, primary_key=True)
    user_id = Column(String(255), nullable=False)
    timestamp = Column(DateTime, default=datetime.utcnow)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    waste_category = Column(String(100), nullable=False)
    waste_sub_type = Column(String(100), nullable=False)
    points_awarded = Column(Integer, nullable=False)
    
    def __repr__(self):
        return f'<Disposal {self.user_id} - {self.waste_category}>'

class DailyDisposalLog(db.Model):
    __tablename__ = 'daily_disposal_log'
    
    id = Column(Integer, primary_key=True)
    user_id = Column(String(255), nullable=False)
    date = Column(Date, nullable=False)
    waste_sub_type = Column(String(100), nullable=False)
    
    def __repr__(self):
        return f'<DailyLog {self.user_id} - {self.date} - {self.waste_sub_type}>'

class Challenge(db.Model):
    __tablename__ = 'challenges'
    
    id = Column(Integer, primary_key=True)
    challenge_id = Column(String(100), unique=True, nullable=False)
    challenge_type = Column(String(50), nullable=False)  # COUNT, VARIETY, HOTSPOT
    category = Column(String(100))  # For category-specific challenges
    goal = Column(Integer, nullable=False)
    reward = Column(Integer, nullable=False)
    description = Column(Text, nullable=False)
    
    def __repr__(self):
        return f'<Challenge {self.challenge_id}>'

class UserChallengeProgress(db.Model):
    __tablename__ = 'user_challenge_progress'
    
    id = Column(Integer, primary_key=True)
    user_id = Column(String(255), nullable=False)
    challenge_id = Column(String(100), nullable=False)
    current_progress = Column(Integer, default=0)
    is_completed = Column(Boolean, default=False)
    assigned_date = Column(DateTime, default=datetime.utcnow)
    
    def __repr__(self):
        return f'<UserChallengeProgress {self.user_id} - {self.challenge_id}>'

class Hotspot(db.Model):
    __tablename__ = 'hotspots'
    
    id = Column(Integer, primary_key=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    intensity = Column(Float, nullable=False)  # 0.0 to 1.0
    created_at = Column(DateTime, default=datetime.utcnow)
    expires_at = Column(DateTime, nullable=False)
    
    def __repr__(self):
        return f'<Hotspot {self.latitude}, {self.longitude}>'
