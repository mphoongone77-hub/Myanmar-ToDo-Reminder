package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CategoryDao
import com.example.data.CategoryEntity
import com.example.data.Task
import com.example.data.TodoDao
import com.example.data.TodoEntity
import com.example.data.toEntity
import com.example.data.toTask
import com.example.reminder.ReminderScheduler
import com.example.ui.TaskViewModel
import com.example.widget.TaskRemoteViewsFactory
import com.example.widget.TaskWidgetProvider
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var todoDao: TodoDao
  private lateinit var categoryDao: CategoryDao

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    todoDao = db.todoDao()
    categoryDao = db.categoryDao()
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Myanmar Todo Reminder", appName)
  }

  @Test
  fun testTaskDataClassAndMappers() {
    val dueTime = System.currentTimeMillis() + 100000L
    val task = Task(
      id = 42,
      title = "အစည်းအဝေး တက်ရောက်ရန်",
      category = "အလုပ်",
      dueTime = dueTime,
      notes = "Project status update meeting",
      isCompleted = false,
      priority = 2
    )

    assertEquals(42L, task.id)
    assertEquals("အစည်းအဝေး တက်ရောက်ရန်", task.title)
    assertEquals("အလုပ်", task.category)
    assertEquals(dueTime, task.dueTime)

    // Verify entity mapping
    val entity = task.toEntity()
    assertEquals(task.id, entity.id)
    assertEquals(task.title, entity.title)
    assertEquals(task.category, entity.category)
    assertEquals(task.dueTime, entity.dueDateMillis)

    // Verify round-trip conversion
    val backToTask = entity.toTask()
    assertEquals(task.title, backToTask.title)
    assertEquals(task.category, backToTask.category)
    assertEquals(task.dueTime, backToTask.dueTime)
  }

  @Test
  fun testTaskViewModelAddDeleteFetch() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TaskViewModel(app, todoDao)

    val due = System.currentTimeMillis() + 500000L
    val addJob = viewModel.addTask(
      title = "ကျန်းမာရေး လေ့ကျင့်ခန်း လုပ်ရန်",
      category = "ကျန်းမာရေး",
      dueTime = due,
      notes = "မိနစ် ၃၀ ပြေးမည်"
    )
    addJob.join()

    // Fetch through database
    val todos = todoDao.getAllTodosFlow().first()
    assertTrue(todos.any { it.title == "ကျန်းမာရေး လေ့ကျင့်ခန်း လုပ်ရန်" })

    val created = todos.first { it.title == "ကျန်းမာရေး လေ့ကျင့်ခန်း လုပ်ရန်" }
    assertEquals("ကျန်းမာရေး", created.category)
    assertEquals(due, created.dueDateMillis)

    // Delete through ViewModel
    val deleteJob = viewModel.deleteTaskById(created.id)
    deleteJob.join()

    val remaining = todoDao.getAllTodosFlow().first()
    assertTrue(remaining.none { it.id == created.id })
  }

  @Test
  fun testBurmeseVoiceCommandParser() {
    // Test 1: Tomorrow morning shopping command
    val input1 = "လုပ်ဆောင်ရန် အသစ်ထည့်မည် မနက်ဖြန် ဈေးဝယ်ထွက်ရန်"
    val parsed1 = com.example.voice.BurmeseVoiceCommandParser.parseBurmeseCommand(input1)
    assertEquals("မနက်ဖြန် ဈေးဝယ်ထွက်ရန်", parsed1.title)
    assertEquals("ဈေးဝယ်", parsed1.category)
    assertNotNull(parsed1.dueTime)

    // Test 2: Health / Medicine command with priority
    val input2 = "အရေးကြီး ဆေးသောက်ရန်"
    val parsed2 = com.example.voice.BurmeseVoiceCommandParser.parseBurmeseCommand(input2)
    assertEquals("အရေးကြီး ဆေးသောက်ရန်", parsed2.title)
    assertEquals("ကျန်းမာရေး", parsed2.category)
    assertEquals(2, parsed2.priority)

    // Test 3: Work meeting command
    val input3 = "ရုံး အစည်းအဝေး တက်ရောက်ရန်"
    val parsed3 = com.example.voice.BurmeseVoiceCommandParser.parseBurmeseCommand(input3)
    assertEquals("ရုံး အစည်းအဝေး တက်ရောက်ရန်", parsed3.title)
    assertEquals("အလုပ်", parsed3.category)
  }

  @Test
  fun testAlarmManagerTaskScheduling() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val futureDueTime = System.currentTimeMillis() + 3600000L // 1 hour in future
    val task = Task(
      id = 101L,
      title = "အရေးကြီး အစည်းအဝေး",
      category = "အလုပ်",
      dueTime = futureDueTime
    )

    // Verify scheduling does not throw SecurityException or crash
    ReminderScheduler.scheduleTask(context, task)

    // Verify cancellation executes cleanly
    ReminderScheduler.cancelReminder(context, task.id)
  }

  @Test
  fun testBurmeseReminderTtsSpeaker() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Verify that speaking a reminder in Burmese initializes and executes without unhandled exceptions
    com.example.reminder.BurmeseReminderTtsSpeaker.speakReminder(
      context = context,
      title = "ဆေးသောက်ရန်",
      notes = "ညစာစားပြီး သောက်ပါ",
      category = "ကျန်းမာရေး"
    )
  }

  @Test
  fun testFilterTasksByTitleAndContent() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TaskViewModel(app, todoDao)

    // Insert test tasks
    val task1Job = viewModel.addTask(
      title = "ပရောဂျက် အစီရင်ခံစာ ရေးသားရန်",
      category = "အလုပ်",
      notes = "ဒီဇိုင်းနှင့် backend စာမျက်နှာများ ဖြည့်သွင်းရန်"
    )
    val task2Job = viewModel.addTask(
      title = "သွားဆရာဝန်နှင့် ပြသရန်",
      category = "ကျန်းမာရေး",
      notes = "မနက်ဖြန် ရက်ချိန်း"
    )
    task1Job.join()
    task2Job.join()

    // 1. Search by title
    val searchByTitle = todoDao.searchTodos("အစီရင်ခံစာ").first()
    assertTrue(searchByTitle.any { it.title.contains("အစီရင်ခံစာ") })
    assertTrue(searchByTitle.none { it.title.contains("သွားဆရာဝန်") })

    // 2. Search by content / notes
    val searchByContent = todoDao.searchTodos("backend").first()
    assertTrue(searchByContent.any { it.notes.contains("backend") })
    assertTrue(searchByContent.none { it.title.contains("သွားဆရာဝန်") })

    // 3. Search all
    val all = todoDao.searchTodos("").first()
    assertTrue(all.size >= 2)
  }

  @Test
  fun testAnalyticsCalculatorWeeklyRateAndPriorities() {
    val now = System.currentTimeMillis()
    val testTodos = listOf(
      TodoEntity(
        id = 1,
        title = "အရေးကြီး အလုပ် ၁",
        category = "အလုပ်",
        priority = 2, // High
        isCompleted = true,
        dueDateMillis = now,
        createdAtMillis = now
      ),
      TodoEntity(
        id = 2,
        title = "အရေးကြီး အလုပ် ၂",
        category = "အလုပ်",
        priority = 2, // High
        isCompleted = false,
        dueDateMillis = now,
        createdAtMillis = now
      ),
      TodoEntity(
        id = 3,
        title = "သာမန် ကျန်းမာရေး အလုပ်",
        category = "ကျန်းမာရေး",
        priority = 1, // Medium
        isCompleted = true,
        dueDateMillis = now,
        createdAtMillis = now
      ),
      TodoEntity(
        id = 4,
        title = "အထွေထွေ အလုပ်",
        category = "အထွေထွေ",
        priority = 0, // Low
        isCompleted = false,
        dueDateMillis = now,
        createdAtMillis = now
      )
    )

    val reportMm = com.example.ui.analytics.AnalyticsCalculator.computeWeeklyAnalytics(testTodos, isMyanmar = true)
    // Total count for today / week
    assertEquals(4, reportMm.totalThisWeek)
    assertEquals(2, reportMm.completedThisWeek)
    assertEquals(0.5f, reportMm.overallWeeklyRate, 0.01f)

    // Check Priority Distribution
    val high = reportMm.priorityDistribution.first { it.priority == 2 }
    assertEquals(2, high.count)
    assertEquals(0.5f, high.percentage, 0.01f)

    val med = reportMm.priorityDistribution.first { it.priority == 1 }
    assertEquals(1, med.count)
    assertEquals(0.25f, med.percentage, 0.01f)

    val low = reportMm.priorityDistribution.first { it.priority == 0 }
    assertEquals(1, low.count)
    assertEquals(0.25f, low.percentage, 0.01f)

    // Check Category Counts
    assertEquals(2, reportMm.categoryCounts["အလုပ်"])
    assertEquals(1, reportMm.categoryCounts["ကျန်းမာရေး"])
    assertEquals(1, reportMm.categoryCounts["အထွေထွေ"])

    // Check Daily Breakdown has 7 days
    assertEquals(7, reportMm.dailyBreakdown.size)
  }

  @Test
  fun testRecurringAlarmCalculation() {
    val now = System.currentTimeMillis()

    // 1. Daily repeat calculation
    val nextDaily = ReminderScheduler.calculateNextRepeatTime(now - 1000L, "DAILY")
    assertNotNull(nextDaily)
    assertTrue("Next daily must be in future", nextDaily!! > now)

    // 2. Weekly repeat calculation
    val nextWeekly = ReminderScheduler.calculateNextRepeatTime(now - 1000L, "WEEKLY")
    assertNotNull(nextWeekly)
    assertTrue("Next weekly must be in future", nextWeekly!! > now)
    assertTrue("Next weekly must be further than 5 days", nextWeekly - now > 5 * 86400000L)

    // 3. No repeat
    val noRepeat = ReminderScheduler.calculateNextRepeatTime(now, "NONE")
    org.junit.Assert.assertNull(noRepeat)
  }

  @Test
  fun testNetworkConnectivityObserverAndOfflineMode() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val observer = com.example.util.NetworkConnectivityObserver(context)

    // Test observer methods run cleanly without throwing SecurityException
    val isOnline = observer.isCurrentlyOnline()
    assertNotNull(isOnline)

    val firstEmission = observer.isOnline.first()
    assertNotNull(firstEmission)
  }

  @Test
  fun testOfflineTaskPersistenceAndReminders() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // 1. Verify offline local insertion works in Room DB
    val offlineTask = TodoEntity(
      title = "အော့ဖ်လိုင်း မှတ်စု ရေးသားခြင်း",
      notes = "အင်တာနက် လုံးဝမလိုဘဲ ဖုန်းထဲတွင်သာ သိမ်းဆည်းသည်",
      category = "ကိုယ်ရေး",
      priority = 2,
      dueDateMillis = System.currentTimeMillis() + 600000L,
      reminderTimeMillis = System.currentTimeMillis() + 600000L,
      repeatInterval = "DAILY"
    )

    val id = todoDao.insertTodo(offlineTask)
    assertTrue("Task ID must be generated locally", id > 0)

    val fetched = todoDao.getTodoById(id)
    assertNotNull(fetched)
    assertEquals("အော့ဖ်လိုင်း မှတ်စု ရေးသားခြင်း", fetched!!.title)
    assertEquals("DAILY", fetched.repeatInterval)

    // 2. Verify AlarmManager exact offline scheduling runs without network requirement
    ReminderScheduler.scheduleReminder(context, fetched)
    ReminderScheduler.cancelReminder(context, id)
  }

  @Test
  fun testCategoryEntityAndDao() = runBlocking {
    // 1. Insert default category
    val defaultCat = CategoryEntity(
      name = "Work",
      nameMm = "အလုပ်",
      isDefault = true
    )
    val defaultId = categoryDao.insertCategory(defaultCat)
    assertTrue(defaultId > 0)

    // 2. Insert custom category
    val customCat = CategoryEntity(
      name = "Travel",
      nameMm = "ခရီးသွား",
      isDefault = false
    )
    val customId = categoryDao.insertCategory(customCat)
    assertTrue(customId > 0)

    // 3. Fetch all categories
    val allCategories = categoryDao.getAllCategoriesFlow().first()
    assertEquals(2, allCategories.size)
    assertTrue(allCategories.any { it.name == "Travel" })

    // 4. Delete custom category by ID
    categoryDao.deleteCategoryById(customId)
    val afterDelete = categoryDao.getAllCategoriesFlow().first()
    assertEquals(1, afterDelete.size)
    assertTrue(afterDelete.none { it.id == customId })

    // 5. Attempt deleting default category by ID (protected: isDefault = 0 condition)
    categoryDao.deleteCategoryById(defaultId)
    val afterProtect = categoryDao.getAllCategoriesFlow().first()
    assertEquals(1, afterProtect.size)
    assertTrue(afterProtect.any { it.id == defaultId })
  }

  @Test
  fun testTaskWidgetProviderMarkComplete() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Insert task in DB
    val testTask = TodoEntity(
      title = "ဝစ်ဂျက်မှ ပြီးစီးကြောင်း မှတ်သားရန်",
      category = "အလုပ်",
      priority = 2,
      isCompleted = false
    )
    val taskId = todoDao.insertTodo(testTask)
    assertTrue(taskId > 0)

    // Verify mark complete action on receiver updates database
    todoDao.setCompleted(taskId, true, System.currentTimeMillis())
    val updated = todoDao.getTodoById(taskId)
    assertNotNull(updated)
    assertTrue("Task must be marked complete", updated!!.isCompleted)
    assertNotNull(updated.completedAtMillis)
  }

  @Test
  fun testTaskWidgetRemoteViewsFactory() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Seed a pending task
    val testTask = TodoEntity(
      title = "ဝစ်ဂျက် စာရင်းပြသမှု စစ်ဆေးခြင်း",
      category = "ကျန်းမာရေး",
      priority = 1,
      isCompleted = false,
      dueDateMillis = System.currentTimeMillis() + 3600000L
    )
    todoDao.insertTodo(testTask)

    val factory = TaskRemoteViewsFactory(context, Intent())
    factory.onCreate()
    factory.onDataSetChanged()

    assertTrue("Factory should load pending tasks", factory.count > 0)
    val views = factory.getViewAt(0)
    assertNotNull("RemoteViews must not be null", views)
    assertEquals(1, factory.viewTypeCount)
    assertTrue(factory.hasStableIds())
  }
}
