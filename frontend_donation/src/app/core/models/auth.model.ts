export type RolNombre = 'USUARIO' | 'ADMINISTRADOR';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  name: string;
  password: string;
}

export interface UpdateProfileRequest {
  name: string;
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface CreateUserRequest {
  email: string;
  name: string;
  password: string;
  rol: RolNombre;
}

export interface UpdateUserRequest {
  name?: string;
  email?: string;
  rol?: RolNombre;
  active?: boolean;
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  imageUrl: string | null;
  active: boolean;
  createdAccount: string;
  rolName: RolNombre | string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
  expiration: string;
}
