package com.horarios.SGH;

import com.horarios.SGH.Controller.usersController;
import com.horarios.SGH.Model.User;
import com.horarios.SGH.Repository.IUserRepository;
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
    private IUserRepository usersRepository;

    @Test
    public void testGetUserByIdSuccess() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setFirstName("Test");
        user.setLastName("User");

        when(usersService.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    public void testGetUserByIdNotFound() throws Exception {
        when(usersService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    public void testDeleteUserSuccess() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");

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