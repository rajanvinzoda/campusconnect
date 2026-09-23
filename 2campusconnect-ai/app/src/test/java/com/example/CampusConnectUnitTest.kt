package com.example

import com.example.data.model.PostType
import com.example.data.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class CampusConnectUnitTest {

    @Test
    fun userRole_badgeColors_areValid() {
        val studentColor = UserRole.STUDENT.badgeColor
        val facultyColor = UserRole.FACULTY.badgeColor
        val adminColor = UserRole.UNIVERSITY_ADMIN.badgeColor
        val superAdminColor = UserRole.SUPER_ADMIN.badgeColor

        assertTrue(studentColor != 0L)
        assertTrue(facultyColor != 0L)
        assertTrue(adminColor != 0L)
        assertTrue(superAdminColor != 0L)
    }

    @Test
    fun userRole_displayNames_areCorrect() {
        assertEquals("Super Admin", UserRole.SUPER_ADMIN.displayName)
        assertEquals("Student", UserRole.STUDENT.displayName)
        assertEquals("Faculty", UserRole.FACULTY.displayName)
        assertEquals("Alumni", UserRole.ALUMNI.displayName)
        assertEquals("Club Leader", UserRole.CLUB_LEADER.displayName)
        assertEquals("Department Admin", UserRole.DEPARTMENT_ADMIN.displayName)
        assertEquals("University Admin", UserRole.UNIVERSITY_ADMIN.displayName)
    }

    @Test
    fun serverConfig_defaultValues_areValid() {
        val config = com.example.data.repository.ServerConfig()
        assertTrue(config.serverUrl.startsWith("https://"))
        assertTrue(config.isAutoSyncEnabled)
        assertEquals(10, config.autoSyncIntervalSec)
    }

    @Test
    fun bottomNavDestinations_containsRequiredTabsInOrder() {
        val items = com.example.ui.components.BottomNavDestination.items
        assertEquals(4, items.size)
        assertEquals("feed", items[0].route)
        assertEquals("Feed", items[0].title)
        assertEquals("chat", items[1].route)
        assertEquals("Chat", items[1].title)
        assertEquals("forums", items[2].route)
        assertEquals("Forums", items[2].title)
        assertEquals("career", items[3].route)
        assertEquals("Career Center", items[3].title)
    }

    @Test
    fun userRole_allSevenRolesAreDefined() {
        val roles = UserRole.values().map { it.name }
        assertEquals(7, roles.size)
        assertTrue(roles.contains("SUPER_ADMIN"))
        assertTrue(roles.contains("UNIVERSITY_ADMIN"))
        assertTrue(roles.contains("DEPARTMENT_ADMIN"))
        assertTrue(roles.contains("CLUB_LEADER"))
        assertTrue(roles.contains("FACULTY"))
        assertTrue(roles.contains("ALUMNI"))
        assertTrue(roles.contains("STUDENT"))
    }

    @Test
    fun postType_allExpectedTypesExist() {
        val types = PostType.values().map { it.name }
        assertTrue(types.contains("TEXT"))
        assertTrue(types.contains("PHOTO"))
        assertTrue(types.contains("EVENT"))
        assertTrue(types.contains("POLL"))
        assertTrue(types.contains("ANNOUNCEMENT"))
        assertTrue(types.contains("RESEARCH"))
        assertTrue(types.contains("ACHIEVEMENT"))
    }

    @Test
    fun socketService_singletonInstance_isNonNull() {
        val socket = com.example.network.SocketService.getInstance()
        assertNotNull(socket)
        assertNotNull(socket.connectionState.value)
    }
}
