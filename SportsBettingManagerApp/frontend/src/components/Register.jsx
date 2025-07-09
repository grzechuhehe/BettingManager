import React, { useState } from 'react';
import API from '../api';
import { useNavigate } from 'react-router-dom';

const Register = () => {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        setIsError(false);
        setMessage('');

        try {
            const response = await API.post('/auth/signup', {
                username,
                email,
                password
            });

            setMessage(response.data.message || 'User registered successfully!');
            setUsername('');
            setEmail('');
            setPassword('');
            navigate('/login');
        } catch (error) {
            setIsError(true);
            if (error.response && error.response.data) {
                setMessage(error.response.data.error || 'An unknown error occurred.');
            } else {
                setMessage('An error occurred while connecting to the server.');
            }
        }
    };

    return (
        <div className="p-4">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Register</h2>
            <form onSubmit={handleRegister} className="space-y-4">
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
                    <label htmlFor="email" className="block text-sm font-medium text-gray-700">Email</label>
                    <input
                        type="email"
                        id="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
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
                    Register
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

export default Register;
