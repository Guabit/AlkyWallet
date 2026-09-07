package com.alkywallet.service;

import com.alkywallet.dto.RegisterRequest;
import com.alkywallet.dto.UserRequestDTO;
import com.alkywallet.dto.UserResponseDTO;
import com.alkywallet.dto.UserUpdateDTO;

import java.util.List;

public interface IUserService {

    void registrarUsuario(RegisterRequest request);

    UserResponseDTO crear(UserRequestDTO request);

    UserResponseDTO obtenerPorId(Long id);

    UserResponseDTO obtenerPorEmail(String email);

    List<UserResponseDTO> obtenerTodos();

    UserResponseDTO actualizar(Long id, UserUpdateDTO request);

    UserResponseDTO actualizarPorEmail(String email, UserUpdateDTO request);

    void eliminar(Long id);
}
