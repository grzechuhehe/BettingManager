import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { register as apiRegister } from '../api';

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
            const response = await apiRegister({ username, email, password });
            setMessage(response.data.message || 'Registration successful! You can now log in.');
            setUsername('');
            setEmail('');
            setPassword('');
        } catch (error) {
            setIsError(true);
            if (error.response && error.response.data) {
                setMessage(error.response.data.error || 'Registration failed.');
            } else {
                setMessage('An error occurred while connecting to the server.');
            }
        }
    };

    return (
        <div className="flex flex-col items-center justify-center py-section">
            <div className="surface-card w-full max-w-md animate-scale-in">
                <h2 className="display-sm text-center mb-4">New Registration</h2>
                <p className="body-sm text-muted text-center mb-8">Create your sports trading account.</p>
                <form onSubmit={handleRegister} className="space-y-6">
                    <div>
                        <label htmlFor="username" className="field-label">Username</label>
                        <input type="text" id="username" value={username} onChange={(e) => setUsername(e.target.value)} required className="input-field" />
                    </div>
                    <div>
                        <label htmlFor="email" className="field-label">Email</label>
                        <input type="email" id="email" value={email} onChange={(e) => setEmail(e.target.value)} required className="input-field" />
                    </div>
                    <div>
                        <label htmlFor="password" className="field-label">Password</label>
                        <input type="password" id="password" value={password} onChange={(e) => setPassword(e.target.value)} required className="input-field" />
                    </div>
                    <button type="submit" className="button-primary w-full mt-2">Create Account</button>
                </form>
                {message && (
                    <div className={`mt-8 ${isError ? 'alert-error' : 'alert-success'}`}>{message}</div>
                )}
                <p className="mt-8 text-center body-sm text-muted">
                    Already registered? <Link to="/login" className="text-link">Sign In</Link>
                </p>
            </div>
        </div>
    );
};

export default Register;
