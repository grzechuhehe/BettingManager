import React, { useState } from 'react';
import API from '../api';

const Register = () => {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);

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
            // Clear form on success
            setUsername('');
            setEmail('');
            setPassword('');
        } catch (error) {
            setIsError(true);
            if (error.response && error.response.data) {
                // Use the 'error' field from the backend response
                setMessage(error.response.data.error || 'An unknown error occurred.');
            } else {
                setMessage('An error occurred while connecting to the server.');
            }
        }
    };

    return (
        <div>
            <h2>Register</h2>
            <form onSubmit={handleRegister}>
                <div>
                    <label>Username</label>
                    <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} required />
                </div>
                <div>
                    <label>Email</label>
                    <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                </div>
                <div>
                    <label>Password</label>
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                </div>
                <button type="submit">Register</button>
            </form>
            {message && (
                <p style={{ color: isError ? 'red' : 'green' }}>
                    {message}
                </p>
            )}
        </div>
    );
};

export default Register;
