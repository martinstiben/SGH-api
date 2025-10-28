package com.horarios.SGH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.horarios.SGH.Controller.AuthController;
import com.horarios.SGH.DTO.LoginRequestDTO;
import com.horarios.SGH.Model.Role;
import com.horarios.SGH.Service.AuthService;
import com.horarios.SGH.Service.TokenRevocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(AuthTestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private com.horarios.SGH.Service.usersService usersService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testLoginSuccess() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(authService.initiateLogin(any(LoginRequestDTO.class))).thenReturn("Código de verificación enviado al correo electrónico");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Código de verificación enviado al correo electrónico"));
    }

    @Test
    public void testLoginFailure() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(authService.initiateLogin(any(LoginRequestDTO.class)))
                .thenThrow(new RuntimeException("Credenciales inválidas"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        when(authService.register(any(String.class), any(String.class), any(String.class), any(Role.class))).thenReturn("Usuario registrado correctamente");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Juan Pérez\",\"email\":\"test@example.com\",\"password\":\"password\",\"role\":\"MAESTRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario registrado correctamente"));
    }

    @Test
    public void testRegisterFailure() throws Exception {
        when(authService.register(any(String.class), any(String.class), any(String.class), any(Role.class)))
                .thenThrow(new IllegalStateException("Usuario ya existe"));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Juan Pérez\",\"email\":\"test@example.com\",\"password\":\"password\",\"role\":\"MAESTRO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLogoutSuccess() throws Exception {
        doNothing().when(tokenRevocationService).revokeToken("jwt-token");

        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sesión cerrada exitosamente"));
    }

    @Test
    public void testLogoutNoToken() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetProfile() throws Exception {
        com.horarios.SGH.Model.users user = new com.horarios.SGH.Model.users();
        com.horarios.SGH.Model.People person = new com.horarios.SGH.Model.People();
        person.setFullName("Juan Pérez");
        person.setEmail("test@example.com");
        user.setUserId(1);
        user.setPerson(person);
        user.setPasswordHash("password");
        com.horarios.SGH.Model.Roles role = new com.horarios.SGH.Model.Roles();
        role.setRoleName("MAESTRO");
        user.setRole(role);
        when(authService.getProfile()).thenReturn(user);

        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Pérez"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void testUpdateProfileSuccess() throws Exception {
        doNothing().when(authService).updateUserName("newname");

        mockMvc.perform(multipart("/auth/profile")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("name", "newname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"));
    }

    @Test
    public void testUpdateProfileEmptyName() throws Exception {
        mockMvc.perform(multipart("/auth/profile")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("name", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe proporcionar al menos un campo para actualizar"));
    }
}