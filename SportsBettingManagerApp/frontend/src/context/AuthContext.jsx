import React, { createContext, useState, useEffect, useContext } from 'react';
import API from '../api';
import { useNavigate } from 'react-router-dom';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
            const userData = JSON.parse(storedUser);
            setUser(userData.username);
            setToken(userData.token);
            setIsAuthenticated(true);
            // Set default Authorization header for Axios
            API.defaults.headers.common['Authorization'] = `Bearer ${userData.token}`;
        }
    }, []);

    const login = (userData) => {
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData.username);
        setToken(userData.token);
        setIsAuthenticated(true);
        API.defaults.headers.common['Authorization'] = `Bearer ${userData.token}`;
        navigate('/dashboard'); // Redirect to dashboard after login
    };

    const logout = () => {
        localStorage.removeItem('user');
        setUser(null);
        setToken(null);
        setIsAuthenticated(false);
        delete API.defaults.headers.common['Authorization'];
        navigate('/login'); // Redirect to login after logout
    };

    return (
        <AuthContext.Provider value={{ user, token, isAuthenticated, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    return useContext(AuthContext);
};
