import React from 'react';
import { useAuth } from '../context/AuthContext';

const Dashboard = () => {
    const { user, logout } = useAuth();

    return (
        <div className="p-4 text-center">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Welcome, {user}!</h2>
            <button
                onClick={logout}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
            >
                Logout
            </button>
        </div>
    );
};

export default Dashboard;
