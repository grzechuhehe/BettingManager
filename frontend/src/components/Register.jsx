import React, { useState } from 'react';
import { register as apiRegister } from '../api';
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
            const response = await apiRegister({
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
        <div className="flex flex-col items-center justify-center py-20">
            <div className="surface-card w-full max-w-md shadow-2xl">
                <div className="flex justify-center mb-8">
                    <div className="w-12 h-12 bg-primary rounded-full flex items-center justify-center text-canvas text-2xl font-black">●</div>
                </div>
                <h2 className="display-sm text-center mb-10 uppercase tracking-tighter">New Registration</h2>
                <form onSubmit={handleRegister} className="space-y-6">
                    <div>
                        <label htmlFor="username" className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Username</label>
                        <input
                            type="text"
                            id="username"
                            placeholder="trader_name"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            className="input-field"
                        />
                    </div>
                    <div>
                        <label htmlFor="email" className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Email Endpoint</label>
                        <input
                            type="email"
                            id="email"
                            placeholder="trader@market.io"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            className="input-field"
                        />
                    </div>
                    <div>
                        <label htmlFor="password" className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Security Key</label>
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
                        Create Market Account
                    </button>
                </form>
                {message && (
                    <div className={`mt-8 p-4 rounded-lg border text-center text-[10px] font-bold uppercase tracking-widest ${isError ? 'bg-rose-500/10 border-rose-500/50 text-rose-500' : 'bg-primary/10 border-primary/50 text-primary'}`}>
                        {message}
                    </div>
                )}
                <p className="mt-8 text-center text-xs text-muted">
                    Already registered? <a href="/login" className="text-primary hover:underline font-bold">Login</a>
                </p>
            </div>
        </div>
    );
};

export default Register;
