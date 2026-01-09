import React, { useState } from 'react';
import { login as apiLogin } from '../api';
import { useAuth } from '../context/AuthContext';

const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);
    const { login } = useAuth();

    const handleLogin = async (e) => {
        e.preventDefault();
        setIsError(false);
        setMessage('');

        try {
            const response = await apiLogin({
                username,
                password
            });

            if (response.data.token) {
                login(response.data);
                setMessage('Login successful!');
                setUsername('');
                setPassword('');
            }
        } catch (error) {
            setIsError(true);
            if (error.response && error.response.data) {
                setMessage(error.response.data.error || 'Login failed. Please check your credentials.');
            } else {
                setMessage('An error occurred while connecting to the server.');
            }
        }
    };

    return (
        <div className="p-4">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Login</h2>
            <form onSubmit={handleLogin} className="space-y-4">
                <div>
                    <label htmlFor="username" className="block text-sm font-medium text-gray-700">Username</label>
                    <input
                        type="text"
                        id="username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                    />
                </div>
                <div>
                    <label htmlFor="password" className="block text-sm font-medium text-gray-700">Password</label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                    />
                </div>
                <button
                    type="submit"
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                >
                    Login
                </button>
            </form>
            {message && (
                <p className={`mt-4 text-center ${isError ? 'text-red-500' : 'text-green-500'}`}>
                    {message}
                </p>
            )}
        </div>
    );
};

export default Login;
