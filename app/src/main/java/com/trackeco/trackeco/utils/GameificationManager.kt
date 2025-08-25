package com.trackeco.trackeco.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.trackeco.trackeco.data.models.DailyChallenge
import com.trackeco.trackeco.data.models.WasteRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GameificationManager"
        private val Context.skillsDataStore: DataStore<Preferences> by preferencesDataStore(name = "skills_prefs")
        
        // Skills system keys
        private val RECYCLING_SKILL_XP = intPreferencesKey("recycling_skill_xp")
        private val DISCOVERY_SKILL_XP = intPreferencesKey("discovery_skill_xp")
        private val CONSISTENCY_SKILL_XP = intPreferencesKey("consistency_skill_xp")
        private val ENVIRONMENTAL_SKILL_XP = intPreferencesKey("environmental_skill_xp")
        
        // Achievement keys
        private val FIRST_DISPOSAL_UNLOCKED = booleanPreferencesKey("first_disposal_unlocked")
        private val WEEK_STREAK_UNLOCKED = booleanPreferencesKey("week_streak_unlocked")
        private val PLASTIC_HUNTER_UNLOCKED = booleanPreferencesKey("plastic_hunter_unlocked")
        private val ECO_WARRIOR_UNLOCKED = booleanPreferencesKey("eco_warrior_unlocked")
        private val DIVERSITY_MASTER_UNLOCKED = booleanPreferencesKey("diversity_master_unlocked")
        
        // Stats tracking
        private val TOTAL_PLASTIC_DISPOSED = intPreferencesKey("total_plastic_disposed")
        private val TOTAL_GLASS_DISPOSED = intPreferencesKey("total_glass_disposed")
        private val TOTAL_METAL_DISPOSED = intPreferencesKey("total_metal_disposed")
        private val TOTAL_PAPER_DISPOSED = intPreferencesKey("total_paper_disposed")
        private val TOTAL_ORGANIC_DISPOSED = intPreferencesKey("total_organic_disposed")
        private val UNIQUE_SUBTYPES_DISCOVERED = stringSetPreferencesKey("unique_subtypes_discovered")
        
        // Crisis intervention tracking
        private val POLLUTION_BOTTLES_PREVENTED = intPreferencesKey("pollution_bottles_prevented")
        private val CO2_EMISSIONS_PREVENTED = floatPreferencesKey("co2_emissions_prevented")
        private val UPSTREAM_INTERVENTIONS = intPreferencesKey("upstream_interventions")
    }

    private val skillsDataStore = context.skillsDataStore

    /**
     * Process waste disposal and update skills/achievements
     */
    suspend fun processWasteDisposal(record: WasteRecord): GameificationResult {
        try {
            val results = mutableListOf<GameificationUpdate>()
            
            // 1. Update skills
            val skillUpdates = updateSkills(record)
            results.addAll(skillUpdates)
            
            // 2. Check for new achievements
            val achievementUpdates = checkAchievements(record)
            results.addAll(achievementUpdates)
            
            // 3. Update category statistics
            updateCategoryStats(record)
            
            // 4. Update crisis intervention metrics
            val crisisUpdates = updateCrisisMetrics(record)
            results.addAll(crisisUpdates)
            
            return GameificationResult(
                updates = results,
                bonusPoints = calculateBonusPoints(results),
                newSkillLevels = results.filterIsInstance<GameificationUpdate.SkillLevelUp>()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing gamification", e)
            return GameificationResult(emptyList(), 0, emptyList())
        }
    }

    private suspend fun updateSkills(record: WasteRecord): List<GameificationUpdate> {
        val updates = mutableListOf<GameificationUpdate>()
        val prefs = skillsDataStore.data.first()
        
        skillsDataStore.edit { preferences ->
            // Recycling Skill (based on quantity and category)
            val recyclingXpGain = calculateRecyclingXp(record.category, record.quantity)
            val currentRecyclingXp = preferences[RECYCLING_SKILL_XP] ?: 0
            val newRecyclingXp = currentRecyclingXp + recyclingXpGain
            preferences[RECYCLING_SKILL_XP] = newRecyclingXp
            
            val recyclingLevelUp = checkSkillLevelUp("Recycling", currentRecyclingXp, newRecyclingXp)
            recyclingLevelUp?.let { updates.add(it) }
            
            // Discovery Skill (based on new subtypes)
            val discoveredSubtypes = preferences[UNIQUE_SUBTYPES_DISCOVERED] ?: emptySet()
            if (!discoveredSubtypes.contains(record.subtype)) {
                val newDiscoveredSubtypes = discoveredSubtypes + record.subtype
                preferences[UNIQUE_SUBTYPES_DISCOVERED] = newDiscoveredSubtypes
                
                val discoveryXpGain = 25 // Bonus for discovering new subtype
                val currentDiscoveryXp = preferences[DISCOVERY_SKILL_XP] ?: 0
                val newDiscoveryXp = currentDiscoveryXp + discoveryXpGain
                preferences[DISCOVERY_SKILL_XP] = newDiscoveryXp
                
                updates.add(GameificationUpdate.NewDiscovery(record.subtype, discoveryXpGain))
                
                val discoveryLevelUp = checkSkillLevelUp("Discovery", currentDiscoveryXp, newDiscoveryXp)
                discoveryLevelUp?.let { updates.add(it) }
            }
            
            // Environmental Impact Skill (based on category impact)
            val environmentalXpGain = calculateEnvironmentalImpact(record.category, record.quantity)
            val currentEnvironmentalXp = preferences[ENVIRONMENTAL_SKILL_XP] ?: 0
            val newEnvironmentalXp = currentEnvironmentalXp + environmentalXpGain
            preferences[ENVIRONMENTAL_SKILL_XP] = newEnvironmentalXp
            
            val environmentalLevelUp = checkSkillLevelUp("Environmental", currentEnvironmentalXp, newEnvironmentalXp)
            environmentalLevelUp?.let { updates.add(it) }
        }
        
        return updates
    }

    private suspend fun checkAchievements(record: WasteRecord): List<GameificationUpdate> {
        val updates = mutableListOf<GameificationUpdate>()
        val prefs = skillsDataStore.data.first()
        
        skillsDataStore.edit { preferences ->
            // First Disposal Achievement
            if (preferences[FIRST_DISPOSAL_UNLOCKED] != true) {
                preferences[FIRST_DISPOSAL_UNLOCKED] = true
                updates.add(GameificationUpdate.AchievementUnlocked(
                    "Planet Rescuer",
                    "Your first step toward saving the planet!",
                    50
                ))
            }
            
            // Plastic Hunter Achievement (10 plastic items)
            if (record.category.lowercase() == "plastic" && preferences[PLASTIC_HUNTER_UNLOCKED] != true) {
                val totalPlastic = (preferences[TOTAL_PLASTIC_DISPOSED] ?: 0) + record.quantity
                if (totalPlastic >= 10) {
                    preferences[PLASTIC_HUNTER_UNLOCKED] = true
                    updates.add(GameificationUpdate.AchievementUnlocked(
                        "Plastic Hunter",
                        "Disposed of 10+ plastic items!",
                        100
                    ))
                }
            }
            
            // Diversity Master Achievement (5 different subtypes)
            val discoveredSubtypes = preferences[UNIQUE_SUBTYPES_DISCOVERED] ?: emptySet()
            if (discoveredSubtypes.size >= 5 && preferences[DIVERSITY_MASTER_UNLOCKED] != true) {
                preferences[DIVERSITY_MASTER_UNLOCKED] = true
                updates.add(GameificationUpdate.AchievementUnlocked(
                    "Diversity Master",
                    "Discovered 5+ different waste types!",
                    150
                ))
            }
        }
        
        return updates
    }

    private suspend fun updateCategoryStats(record: WasteRecord) {
        skillsDataStore.edit { preferences ->
            when (record.category.lowercase()) {
                "plastic" -> {
                    val current = preferences[TOTAL_PLASTIC_DISPOSED] ?: 0
                    preferences[TOTAL_PLASTIC_DISPOSED] = current + record.quantity
                }
                "glass" -> {
                    val current = preferences[TOTAL_GLASS_DISPOSED] ?: 0
                    preferences[TOTAL_GLASS_DISPOSED] = current + record.quantity
                }
                "metal" -> {
                    val current = preferences[TOTAL_METAL_DISPOSED] ?: 0
                    preferences[TOTAL_METAL_DISPOSED] = current + record.quantity
                }
                "paper" -> {
                    val current = preferences[TOTAL_PAPER_DISPOSED] ?: 0
                    preferences[TOTAL_PAPER_DISPOSED] = current + record.quantity
                }
                "organic" -> {
                    val current = preferences[TOTAL_ORGANIC_DISPOSED] ?: 0
                    preferences[TOTAL_ORGANIC_DISPOSED] = current + record.quantity
                }
            }
        }
    }

    private suspend fun updateCrisisMetrics(record: WasteRecord): List<GameificationUpdate> {
        val updates = mutableListOf<GameificationUpdate>()
        
        skillsDataStore.edit { preferences ->
            // Calculate crisis intervention impact
            val bottlesPrevented = when (record.category.lowercase()) {
                "plastic" -> record.quantity * 2 // Each plastic disposal prevents 2 bottles from being produced
                else -> 0
            }
            
            val co2Prevented = when (record.category.lowercase()) {
                "plastic" -> record.quantity * 0.5f // kg CO2 per plastic item
                "glass" -> record.quantity * 0.3f
                "metal" -> record.quantity * 1.2f
                "paper" -> record.quantity * 0.8f
                else -> 0f
            }
            
            if (bottlesPrevented > 0) {
                val current = preferences[POLLUTION_BOTTLES_PREVENTED] ?: 0
                preferences[POLLUTION_BOTTLES_PREVENTED] = current + bottlesPrevented
                
                updates.add(GameificationUpdate.CrisisIntervention(
                    "Pollution Prevention",
                    "$bottlesPrevented plastic bottles prevented from production",
                    bottlesPrevented
                ))
            }
            
            if (co2Prevented > 0) {
                val current = preferences[CO2_EMISSIONS_PREVENTED] ?: 0f
                preferences[CO2_EMISSIONS_PREVENTED] = current + co2Prevented
                
                updates.add(GameificationUpdate.CrisisIntervention(
                    "CO₂ Reduction",
                    "${String.format("%.1f", co2Prevented)}kg CO₂ emissions prevented",
                    co2Prevented.toInt()
                ))
            }
        }
        
        return updates
    }

    private fun calculateRecyclingXp(category: String, quantity: Int): Int {
        val baseXp = when (category.lowercase()) {
            "plastic" -> 15
            "glass" -> 20
            "metal" -> 25
            "paper" -> 10
            "organic" -> 12
            else -> 10
        }
        return baseXp * quantity
    }

    private fun calculateEnvironmentalImpact(category: String, quantity: Int): Int {
        val impactMultiplier = when (category.lowercase()) {
            "plastic" -> 3 // High environmental impact
            "metal" -> 4
            "glass" -> 2
            "paper" -> 1
            "organic" -> 2
            else -> 1
        }
        return 10 * quantity * impactMultiplier
    }

    private fun checkSkillLevelUp(skillName: String, oldXp: Int, newXp: Int): GameificationUpdate.SkillLevelUp? {
        val oldLevel = calculateSkillLevel(oldXp)
        val newLevel = calculateSkillLevel(newXp)
        
        return if (newLevel > oldLevel) {
            GameificationUpdate.SkillLevelUp(
                skillName = skillName,
                newLevel = newLevel,
                bonusPoints = newLevel * 25
            )
        } else null
    }

    private fun calculateSkillLevel(xp: Int): Int {
        return when {
            xp < 100 -> 1
            xp < 250 -> 2
            xp < 500 -> 3
            xp < 1000 -> 4
            xp < 2000 -> 5
            xp < 3500 -> 6
            xp < 5500 -> 7
            xp < 8000 -> 8
            xp < 12000 -> 9
            else -> 10
        }
    }

    private fun calculateBonusPoints(updates: List<GameificationUpdate>): Int {
        return updates.sumOf { update ->
            when (update) {
                is GameificationUpdate.AchievementUnlocked -> update.bonusPoints
                is GameificationUpdate.SkillLevelUp -> update.bonusPoints
                is GameificationUpdate.NewDiscovery -> update.xpGain
                is GameificationUpdate.CrisisIntervention -> update.impactValue
            }
        }
    }

    suspend fun getSkillLevels(): Map<String, Int> {
        val prefs = skillsDataStore.data.first()
        return mapOf(
            "Recycling" to calculateSkillLevel(prefs[RECYCLING_SKILL_XP] ?: 0),
            "Discovery" to calculateSkillLevel(prefs[DISCOVERY_SKILL_XP] ?: 0),
            "Consistency" to calculateSkillLevel(prefs[CONSISTENCY_SKILL_XP] ?: 0),
            "Environmental" to calculateSkillLevel(prefs[ENVIRONMENTAL_SKILL_XP] ?: 0)
        )
    }

    suspend fun getCrisisStats(): CrisisStats {
        val prefs = skillsDataStore.data.first()
        return CrisisStats(
            bottlesPrevented = prefs[POLLUTION_BOTTLES_PREVENTED] ?: 0,
            co2Prevented = prefs[CO2_EMISSIONS_PREVENTED] ?: 0f,
            upstreamInterventions = prefs[UPSTREAM_INTERVENTIONS] ?: 0
        )
    }

    data class GameificationResult(
        val updates: List<GameificationUpdate>,
        val bonusPoints: Int,
        val newSkillLevels: List<GameificationUpdate.SkillLevelUp>
    )

    sealed class GameificationUpdate {
        data class SkillLevelUp(
            val skillName: String,
            val newLevel: Int,
            val bonusPoints: Int
        ) : GameificationUpdate()

        data class AchievementUnlocked(
            val title: String,
            val description: String,
            val bonusPoints: Int
        ) : GameificationUpdate()

        data class NewDiscovery(
            val subtype: String,
            val xpGain: Int
        ) : GameificationUpdate()

        data class CrisisIntervention(
            val type: String,
            val description: String,
            val impactValue: Int
        ) : GameificationUpdate()
    }

    data class CrisisStats(
        val bottlesPrevented: Int,
        val co2Prevented: Float,
        val upstreamInterventions: Int
    )
}