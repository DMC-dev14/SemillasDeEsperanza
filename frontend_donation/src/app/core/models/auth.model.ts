
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  name: string;
  password: string;
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  imageUrl: string | null;
  active: boolean;
  createdAccount: string;
  rolName: string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
  expiration: string;
}
