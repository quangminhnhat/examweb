package com.exam.examweb;

import com.exam.examweb.entities.ClassEntity;
import com.exam.examweb.entities.User;
import com.exam.examweb.repositories.ClassRepository;
import com.exam.examweb.repositories.IUserRepository;
import com.exam.examweb.services.ClassService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ClassApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacher;

    @BeforeEach
    void setUp() {
        classRepository.deleteAll();
        userRepository.deleteAll();

        teacher = new User();
        teacher.setUsername("teacher1");
        teacher.setPassword("password");
        teacher.setEmail("teacher1@example.com");
        teacher.setPhone("1234567890");
        userRepository.save(teacher);
    }

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    public void testCreateClass() throws Exception {
        ClassEntity newClass = new ClassEntity();
        newClass.setClassName("Math 101");
        newClass.setTeacher(teacher);

        mockMvc.perform(post("/api/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newClass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("Math 101"))
                .andExpect(jsonPath("$.inviteCode").exists());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    public void testGetAllClasses() throws Exception {
        ClassEntity class1 = new ClassEntity();
        class1.setClassName("History");
        class1.setTeacher(teacher);
        class1.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        classRepository.save(class1);

        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].className").value("History"));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    public void testGetClassById() throws Exception {
        ClassEntity class1 = new ClassEntity();
        class1.setClassName("Science");
        class1.setTeacher(teacher);
        class1.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        ClassEntity savedClass = classRepository.save(class1);

        mockMvc.perform(get("/api/classes/" + savedClass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("Science"));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    public void testUpdateClass() throws Exception {
        ClassEntity class1 = new ClassEntity();
        class1.setClassName("Old Name");
        class1.setTeacher(teacher);
        class1.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        ClassEntity savedClass = classRepository.save(class1);

        savedClass.setClassName("New Name");

        mockMvc.perform(put("/api/classes/" + savedClass.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(savedClass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("New Name"));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    public void testDeleteClass() throws Exception {
        ClassEntity class1 = new ClassEntity();
        class1.setClassName("To Delete");
        class1.setTeacher(teacher);
        class1.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        ClassEntity savedClass = classRepository.save(class1);

        mockMvc.perform(delete("/api/classes/" + savedClass.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/classes/" + savedClass.getId()))
                .andExpect(status().isNotFound());
    }
}
