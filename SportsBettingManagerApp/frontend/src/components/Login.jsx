import React, { useState } from 'react';
import API from '../api';

const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);

    const handleLogin = async (e) => {
        e.preventDefault();
        setIsError(false);
        setMessage('');

        try {
            const response = await API.post('/auth/signin', {
                username,
                password
            });

            if (response.data.token) {
                localStorage.setItem('user', JSON.stringify(response.data));
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
        <div>
            <h2>Login</h2>
            <form onSubmit={handleLogin}>
                <div>
                    <label>Username</label>
                    <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} required />
                </div>
                <div>
                    <label>Password</label>
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                </div>
                <button type="submit">Login</button>
            </form>
            {message && (
                <p style={{ color: isError ? 'red' : 'green' }}>
                    {message}
                </p>
            )}
        </div>
    );
};

export default Login;
