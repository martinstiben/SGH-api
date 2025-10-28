package com.horarios.SGH;

import com.horarios.SGH.Controller.usersController;
import com.horarios.SGH.Model.users;
import com.horarios.SGH.Repository.Iusers;
import com.horarios.SGH.Service.usersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(usersController.class)
@Import(UsersTestSecurityConfig.class)
public class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private usersService usersService;

    @MockBean
    private Iusers usersRepository;

    @Test
    public void testGetUserByIdSuccess() throws Exception {
        users user = new users();
        com.horarios.SGH.Model.People person = new com.horarios.SGH.Model.People();
        person.setFullName("testuser");
        user.setUserId(1);
        user.setPerson(person);

        when(usersService.findById(1)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.fullName").value("testuser"));
    }

    @Test
    public void testGetUserByIdNotFound() throws Exception {
        when(usersService.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    public void testLoginSuccess() throws Exception {
        users user = new users();
        com.horarios.SGH.Model.People person = new com.horarios.SGH.Model.People();
        person.setEmail("testuser");
        user.setPerson(person);
        user.setPasswordHash("password");

        when(usersRepository.findByUserName("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/users/login")
                .param("userName", "testuser")
                .param("password", "password"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testLoginInvalidUsername() throws Exception {
        mockMvc.perform(post("/users/login")
                .param("userName", "")
                .param("password", "password"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testLoginUserNotFound() throws Exception {
        when(usersRepository.findByUserName("testuser")).thenReturn(Optional.empty());

        mockMvc.perform(post("/users/login")
                .param("userName", "testuser")
                .param("password", "password"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testDeleteUserSuccess() throws Exception {
        users user = new users();
        com.horarios.SGH.Model.People person = new com.horarios.SGH.Model.People();
        person.setEmail("testuser");
        user.setPerson(person);

        when(usersRepository.findByUserName("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/users/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario eliminado correctamente"));
    }

    @Test
    public void testDeleteMasterUser() throws Exception {
        mockMvc.perform(delete("/users/username/master"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No se puede eliminar el usuario master"));
    }

    @Test
    public void testDeleteUserNotFound() throws Exception {
        when(usersRepository.findByUserName("testuser")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/users/username/testuser"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }
}