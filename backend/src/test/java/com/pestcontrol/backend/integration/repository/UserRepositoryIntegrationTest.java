package com.pestcontrol.backend.integration.repository;

import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void saveUser_findReturnsUserSuccessfully() {
        User user = buildUser("alice.integration@test.com", "5145551000");

        User savedUser = userRepository.save(user);
        var fetchedUser = userRepository.findById(savedUser.getUserId());

        assertNotNull(savedUser.getUserId());
        assertTrue(fetchedUser.isPresent());
        assertEquals("Alice Smith", fetchedUser.get().getFullName());
        assertEquals("alice.integration@test.com", fetchedUser.get().getEmail());
        assertEquals(UserRole.CUSTOMER, fetchedUser.get().getUserRole());
    }

    @Test
    void findByEmail_whenUserExists_returnsUser() {
        userRepository.save(buildUser("findbyemail@test.com", "5145551001"));

        var foundUser = userRepository.findByEmail("findbyemail@test.com");

        assertTrue(foundUser.isPresent());
        assertEquals("findbyemail@test.com", foundUser.get().getEmail());
    }

    @Test
    void findByEmail_whenUserDoesNotExist_returnsEmpty() {
        var foundUser = userRepository.findByEmail("missing@test.com");

        assertTrue(foundUser.isEmpty());
    }

    @Test
    void findByPhoneNumber_whenUserExists_returnsUser() {
        userRepository.save(buildUser("findbyphone@test.com", "5145551002"));

        var foundUser = userRepository.findByPhoneNumber("5145551002");

        assertTrue(foundUser.isPresent());
        assertEquals("findbyphone@test.com", foundUser.get().getEmail());
    }

    @Test
    void findByPhoneNumber_whenUserDoesNotExist_returnsEmpty() {
        var foundUser = userRepository.findByPhoneNumber("5145551999");

        assertTrue(foundUser.isEmpty());
    }

    @Test
    void existsByEmail_whenUserExists_returnsTrue() {
        userRepository.save(buildUser("existsbyemail@test.com", "5145551003"));

        assertTrue(userRepository.existsByEmail("existsbyemail@test.com"));
    }

    @Test
    void existsByEmail_whenUserDoesNotExist_returnsFalse() {
        assertFalse(userRepository.existsByEmail("notfound@test.com"));
    }

    @Test
    void existsByPhoneNumber_whenUserExists_returnsTrue() {
        userRepository.save(buildUser("existsbyphone@test.com", "5145551004"));

        assertTrue(userRepository.existsByPhoneNumber("5145551004"));
    }

    @Test
    void existsByPhoneNumber_whenUserDoesNotExist_returnsFalse() {
        assertFalse(userRepository.existsByPhoneNumber("5145551998"));
    }

    @Test
    void saveUser_withDuplicateEmail_throwsDataIntegrityViolationException() {
        userRepository.saveAndFlush(buildUser("duplicate@test.com", "5145551005"));

        User duplicateEmailUser = buildUser("duplicate@test.com", "5145551006");

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicateEmailUser));
    }

    private User buildUser(String email, String phone) {
        return new User("Alice Smith", email, phone, "hashed-password", UserRole.CUSTOMER);
    }
}
