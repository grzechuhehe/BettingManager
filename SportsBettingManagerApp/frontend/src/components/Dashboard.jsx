import React from 'react';
import { useAuth } from '../context/AuthContext';

const Dashboard = () => {
    const { user, logout } = useAuth();

    return (
        <div>
            <h2>Welcome, {user}!</h2>
            <button onClick={logout}>Logout</button>
        </div>
    );
};

export default Dashboard;
