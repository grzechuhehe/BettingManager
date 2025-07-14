import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Dashboard = () => {
    const { user } = useAuth();

    const Card = ({ to, title, description }) => (
        <Link to={to} className="block p-6 bg-white border border-gray-200 rounded-lg shadow-md hover:bg-gray-100 transition-colors duration-300">
            <h5 className="mb-2 text-2xl font-bold tracking-tight text-gray-900">{title}</h5>
            <p className="font-normal text-gray-700">{description}</p>
        </Link>
    );

    const ComingSoonCard = ({ title, description }) => (
        <div className="block p-6 bg-gray-200 border border-gray-300 rounded-lg shadow-md cursor-not-allowed">
            <h5 className="mb-2 text-2xl font-bold tracking-tight text-gray-500">{title}</h5>
            <p className="font-normal text-gray-500">{description}</p>
            <span className="text-xs font-semibold text-gray-400">Coming Soon</span>
        </div>
    );

    return (
        <div className="p-4">
            <h2 className="text-3xl font-bold mb-8 text-gray-800">Welcome, {user}!</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <Card 
                    to="/add-bet"
                    title="Add New Bet"
                    description="Place a new bet on an upcoming event."
                />
                <ComingSoonCard
                    title="UI/UX Settings"
                    description="Customize the look and feel of the application."
                />
                <ComingSoonCard
                    title="User Profile"
                    description="View and edit your profile information."
                />
            </div>
        </div>
    );
};

export default Dashboard;
