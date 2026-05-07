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
        <div className="flex flex-col items-center justify-center py-20">
            <div className="surface-card w-full max-w-md shadow-2xl">
                <div className="flex justify-center mb-8">
                    <div className="w-12 h-12 bg-primary rounded-full flex items-center justify-center text-canvas text-2xl font-black">●</div>
                </div>
                <h2 className="display-sm text-center mb-10 uppercase tracking-tighter">Market Access</h2>
                <form onSubmit={handleLogin} className="space-y-6">
                    <div>
                        <label htmlFor="username" className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Identifier</label>
                        <input
                            type="text"
                            id="username"
                            placeholder="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            className="input-field"
                        />
                    </div>
                    <div>
                        <label htmlFor="password" className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Access Key</label>
                        <input
                            type="password"
                            id="password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="input-field"
                        />
                    </div>
                    <button
                        type="submit"
                        className="button-primary w-full !h-12 text-sm uppercase tracking-widest font-black mt-4"
                    >
                        Initialize Session
                    </button>
                </form>
                {message && (
                    <div className={`mt-8 p-4 rounded-lg border text-center text-[10px] font-bold uppercase tracking-widest ${isError ? 'bg-rose-500/10 border-rose-500/50 text-rose-500' : 'bg-primary/10 border-primary/50 text-primary'}`}>
                        {message}
                    </div>
                )}
                <p className="mt-8 text-center text-xs text-muted">
                    Don't have an account? <a href="/register" className="text-primary hover:underline font-bold">Register</a>
                </p>
            </div>
        </div>
    );
};

export default Login;
