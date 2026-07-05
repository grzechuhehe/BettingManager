import React from 'react';
import { Link } from 'react-router-dom';

const Home = () => {
  return (
    <div className="flex flex-col items-center justify-center py-section text-center">
      <div className="mb-6 badge-yellow animate-fade-in">
        <span>Institutional Grade Trading Tools</span>
      </div>
      <h1 className="display-xl mb-8 max-w-4xl text-on-dark animate-fade-in" style={{ animationDelay: '50ms' }}>
        The leading analytics platform for sports traders.
      </h1>
      <p className="text-xl text-body max-w-2xl mb-14 leading-relaxed animate-fade-in" style={{ animationDelay: '100ms' }}>
        Track your performance, analyze market metrics, and optimize your edge with engineering-grade technical analysis.
      </p>
      <div className="flex flex-col sm:flex-row gap-4 w-full max-w-md justify-center items-center animate-fade-in" style={{ animationDelay: '150ms' }}>
        <Link to="/register" className="button-primary px-8">Get Started</Link>
        <Link to="/login" className="button-text-link px-4">Sign In</Link>
      </div>

      <div className="mt-section grid grid-cols-1 md:grid-cols-3 gap-12 w-full max-w-6xl border-t border-hairline pt-16 stagger-children">
        <div className="text-left">
          <p className="stat-display mb-4">24/7</p>
          <h3 className="title-sm mb-2">Market Tracking</h3>
          <p className="body-sm text-muted">Real-time performance tracking across major global sports markets.</p>
        </div>
        <div className="text-left">
          <p className="stat-display mb-4">100%</p>
          <h3 className="title-sm mb-2">Analytics</h3>
          <p className="body-sm text-muted">Precise settlement engine ensuring your ROI and Yield metrics are accurate.</p>
        </div>
        <div className="text-left">
          <p className="stat-display mb-4">0ms</p>
          <h3 className="title-sm mb-2">Performance</h3>
          <p className="body-sm text-muted">Fast market synchronization and data processing for sports traders.</p>
        </div>
      </div>
    </div>
  );
};

export default Home;
