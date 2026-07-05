import React, { useState } from 'react';
import { Link } from 'react-router-dom';
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
            const response = await apiLogin({ username, password });
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
        <div className="flex flex-col items-center justify-center py-section">
            <div className="surface-card w-full max-w-md animate-scale-in">
                <h2 className="display-sm text-center mb-4">Market Access</h2>
                <p className="body-sm text-muted text-center mb-8">Sign in to your trading account.</p>
                <form onSubmit={handleLogin} className="space-y-6">
                    <div>
                        <label htmlFor="username" className="field-label">Identifier</label>
                        <input type="text" id="username" placeholder="username" value={username} onChange={(e) => setUsername(e.target.value)} required className="input-field" />
                    </div>
                    <div>
                        <label htmlFor="password" className="field-label">Password</label>
                        <input type="password" id="password" placeholder="password" value={password} onChange={(e) => setPassword(e.target.value)} required className="input-field" />
                    </div>
                    <button type="submit" className="button-primary w-full mt-2">Sign In</button>
                </form>
                <div className="mt-4 text-center">
                    <Link to="/forgot-password" className="text-link body-sm">Forgot Password?</Link>
                </div>
                {message && (
                    <div className={`mt-8 ${isError ? 'alert-error' : 'alert-success'}`}>{message}</div>
                )}
                <p className="mt-8 text-center body-sm text-muted">
                    Don&apos;t have an account? <Link to="/register" className="text-link">Register</Link>
                </p>
            </div>
        </div>
    );
};

export default Login;
