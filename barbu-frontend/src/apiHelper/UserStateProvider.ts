import { create } from "zustand";
import type { LoginData, User } from "./UserTypes.ts";
//import { persist } from "zustand/middleware";

const host = import.meta.env.VITE_API_BASE_URL || "localhost:8080";

function getCsrfToken(): string {
  return (
    document.cookie
      .split("; ")
      .find((r) => r.startsWith("XSRF-TOKEN="))
      ?.split("=")[1] ?? ""
  );
}

interface UserState {
  userInfo: User | null;
  isLoading: boolean;
  error: string | null;
  getUserInfo: () => Promise<User | null>;
  setLogin: (data: LoginData) => Promise<boolean>;
  setLogout: () => Promise<boolean>;
  setRegister: (data: LoginData) => Promise<boolean>;
}

export const UserStateProvider = create<UserState>(
  //()(
  //persist(
  //(set, get) => ({
  (set, get) => ({
    userInfo: null,
    isLoading: false,
    error: null,

    //fetch user info
    getUserInfo: async () => {
      if (get().isLoading) return null;
      set({ isLoading: true, error: null });
      try {
        const response = await fetch(`http://${host}/api/users/me`, {
          method: "GET",
          headers: {
            "X-XSRF-TOKEN": getCsrfToken(),
          },
          credentials: "include",
        });

        if (response.ok) {
          const data: User = await response.json();
          set({ userInfo: data, isLoading: false });
        } else if (response.status === 401 || response.status === 403) {
          set({
            error: null,
            isLoading: false,
            userInfo: null,
          });
        } else {
          const errorText = await response.text();
          set({
            error: errorText || "Could not verify user.",
            isLoading: false,
          });
        }
      } catch (error: any) {
        set({
          error: "Cannot connect to server. Please check your connection.",
          //userInfo: null,
          isLoading: false,
        });
      }
      return get().userInfo;
    },

    // login request
    setLogin: async (data: LoginData) => {
      try {
        const response = await fetch(`http://${host}/api/auth/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(data),
          credentials: "include",
        });

        if (response.ok) {
          await UserStateProvider.getState().getUserInfo(); // to get/update user info
        } else if (response.status === 401) {
          set({ error: "Invalid username or password." });
        } else {
          const errorText = await response.text();
          set({ error: errorText || "An error occurred during login." });
        }
      } catch (error) {
        set({
          error: "Cannot connect to server. Please check your connection.",
          isLoading: false,
        });
      }
      return false;
    },

    // logout request
    setLogout: async () => {
      try {
        const response = await fetch(`http://${host}/api/auth/logout`, {
          method: "POST",
          headers: {
            "X-XSRF-TOKEN": getCsrfToken(),
          },
          credentials: "include",
        });

        if (response.ok) {
          set({ userInfo: null });
          return true;
        } else {
          const errorText = await response.text();
          set({
            error: errorText || "An error occurred during logout.",
            isLoading: false,
          });
        }
      } catch (error) {
        set({
          error: "Cannot connect to server. Please check your connection.",
          isLoading: false,
        });
      }
      return false;
    },

    //register request
    setRegister: async (data: LoginData) => {
      try {
        const response = await fetch(`http://${host}/api/auth/register`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(data),
          credentials: "include",
        });

        if (response.ok) {
          await UserStateProvider.getState().getUserInfo(); //to get/update user info
          return true;
        } else {
          if (response.status === 400) {
            set({
              error: "Username or password doesn't match the requirements.",
            });
          } else {
            const errorText = await response.text();
            set({
              error: errorText || "An error occurred during registration.",
            });
          }
        }
      } catch (error) {
        set({
          error: "Cannot connect to server. Please check your connection.",
          isLoading: false,
        });
      }
      return false;
    },
    /*}),
    {
      name: "user-storage",
      partialize: (state) => ({ userInfo: state.userInfo }),
    },
  ),*/
  }),
);
