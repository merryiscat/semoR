package com.semo.alarm.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.semo.alarm.data.entities.TimerCategory

@Dao
interface TimerCategoryDao {
    
    @Query("SELECT * FROM timer_categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): LiveData<List<TimerCategory>>
    
    @Query("SELECT * FROM timer_categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategoriesSync(): List<TimerCategory>
    
    @Query("SELECT * FROM timer_categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): TimerCategory?
    
    @Query("SELECT * FROM timer_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): TimerCategory?
    
    @Query("SELECT * FROM timer_categories WHERE isDefault = 1 ORDER BY sortOrder ASC")
    suspend fun getDefaultCategories(): List<TimerCategory>
    
    @Query("SELECT * FROM timer_categories WHERE createdBy = 'user' ORDER BY sortOrder ASC, createdAt DESC")
    fun getUserCategories(): LiveData<List<TimerCategory>>
    
    @Query("SELECT COUNT(*) FROM timer_categories")
    suspend fun getCategoryCount(): Int
    
    @Query("SELECT MAX(sortOrder) FROM timer_categories")
    suspend fun getMaxSortOrder(): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TimerCategory): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<TimerCategory>): List<Long>
    
    @Update
    suspend fun updateCategory(category: TimerCategory)
    
    @Delete
    suspend fun deleteCategory(category: TimerCategory)
    
    @Query("DELETE FROM timer_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)
    
    @Query("UPDATE timer_categories SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Int, newOrder: Int)
    
    // 카테고리 순서 재정렬
    @Transaction
    suspend fun reorderCategories(categoryIds: List<Int>) {
        categoryIds.forEachIndexed { index, categoryId ->
            updateSortOrder(categoryId, index)
        }
    }
    
    // 카테고리별 템플릿 개수 조회
    @Query("""
        SELECT c.*, 
               COUNT(t.id) as templateCount
        FROM timer_categories c 
        LEFT JOIN timer_templates t ON c.id = t.categoryId 
        GROUP BY c.id 
        ORDER BY c.sortOrder ASC, c.name ASC
    """)
    suspend fun getCategoriesWithTemplateCount(): List<CategoryWithTemplateCount>
    
    // 사용자 정의 카테고리인지 확인
    @Query("SELECT COUNT(*) > 0 FROM timer_categories WHERE id = :id AND createdBy = 'user'")
    suspend fun isUserCategory(id: Int): Boolean
    
    // 기본 카테고리 초기화 여부 확인
    @Query("SELECT COUNT(*) FROM timer_categories WHERE isDefault = 1")
    suspend fun getDefaultCategoryCount(): Int
}

// 카테고리와 템플릿 개수를 함께 반환하는 데이터 클래스
data class CategoryWithTemplateCount(
    @Embedded val category: TimerCategory,
    val templateCount: Int
)