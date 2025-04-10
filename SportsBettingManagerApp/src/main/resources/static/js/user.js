document.addEventListener('DOMContentLoaded', () => {
    // Check if user is logged in
    if (!auth.isAuthenticated()) {
        window.location.href = 'index.html';
        return;
    }

    // Get profile elements
    const usernameDisplay = document.getElementById('username-display');
    const emailDisplay = document.getElementById('email-display');
    const createdDateDisplay = document.getElementById('created-date-display');
    const updateProfileForm = document.getElementById('update-profile-form');
    const usernameInput = document.getElementById('username');
    const emailInput = document.getElementById('email');
    const changePasswordForm = document.getElementById('change-password-form');
    
    // Get statistics elements
    const totalBetsElement = document.getElementById('total-bets');
    const winRateElement = document.getElementById('win-rate');
    const profitLossElement = document.getElementById('profit-loss');
    const avgOddsElement = document.getElementById('avg-odds');

    // Load user data
    loadUserProfile();
    loadUserStatistics();

    // Set up event listeners
    updateProfileForm.addEventListener('submit', handleProfileUpdate);
    changePasswordForm.addEventListener('submit', handlePasswordChange);
    document.getElementById('logout-btn').addEventListener('click', handleLogout);

    // Function to load user profile data
    async function loadUserProfile() {
        try {
            const token = state.getToken();
            const userId = state.getUserId();
            
            // Using stored user data initially
            const userData = state.getUserData();
            if (userData) {
                usernameDisplay.textContent = userData.username || 'Not available';
                emailDisplay.textContent = userData.email || 'Not available';
                usernameInput.value = userData.username || '';
                emailInput.value = userData.email || '';
            }
            
            createdDateDisplay.textContent = 'Loading...';
            
            // Fetch latest data from server
            const response = await fetch(`${config.apiUrl}/api/users/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            if (response.ok) {
                const user = await response.json();
                
                // Update display
                usernameDisplay.textContent = user.username;
                emailDisplay.textContent = user.email;
                createdDateDisplay.textContent = user.createdAt ? 
                    new Date(user.createdAt).toLocaleDateString() : 'Not available';
                
                // Update form fields
                usernameInput.value = user.username;
                emailInput.value = user.email;
                
                // Update stored user data
                state.setUserData({
                    id: user.id,
                    username: user.username,
                    email: user.email,
                    roles: user.roles
                });
            } else {
                createdDateDisplay.textContent = 'Not available';
                console.error('Failed to fetch user data');
            }
            
        } catch (error) {
            console.error('Error loading profile:', error);
            ui.showNotification('Failed to load profile data', 'error');
        }
    }

    // Function to load user betting statistics
    async function loadUserStatistics() {
        try {
            const response = await fetch(`${config.apiUrl}/api/bets/statistics`, {
                headers: {
                    'Authorization': `Bearer ${state.getToken()}`
                }
            });

            if (response.ok) {
                const stats = await response.json();
                
                // Update statistics display
                totalBetsElement.textContent = stats.totalBets || 0;
                winRateElement.textContent = `${(stats.winRate || 0).toFixed(1)}%`;
                profitLossElement.textContent = `$${(stats.profitLoss || 0).toFixed(2)}`;
                avgOddsElement.textContent = (stats.averageOdds || 0).toFixed(2);
                
                // Set color for profit/loss
                if (stats.profitLoss > 0) {
                    profitLossElement.classList.add('bet-status-won');
                    profitLossElement.classList.remove('bet-status-lost');
                } else if (stats.profitLoss < 0) {
                    profitLossElement.classList.add('bet-status-lost');
                    profitLossElement.classList.remove('bet-status-won');
                }
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
            // Silently fail for statistics
        }
    }

    // Handle profile update
    async function handleProfileUpdate(event) {
        event.preventDefault();
        
        const userData = {
            username: usernameInput.value.trim(),
            email: emailInput.value.trim()
        };
        
        if (!userData.username || !userData.email) {
            ui.showNotification('Username and email are required', 'error');
            return;
        }
        
        try {
            const response = await fetch(`${config.apiUrl}/api/users/${state.getUserId()}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${state.getToken()}`
                },
                body: JSON.stringify(userData)
            });
            
            if (response.ok) {
                ui.showNotification('Profile updated successfully', 'success');
                loadUserProfile(); // Reload profile data
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to update profile');
            }
        } catch (error) {
            ui.showNotification(error.message, 'error');
            console.error('Error updating profile:', error);
        }
    }

    // Handle password change
    async function handlePasswordChange(event) {
        event.preventDefault();
        
        const currentPassword = document.getElementById('current-password').value;
        const newPassword = document.getElementById('new-password').value;
        const confirmPassword = document.getElementById('confirm-password').value;
        
        if (!currentPassword || !newPassword || !confirmPassword) {
            ui.showNotification('All password fields are required', 'error');
            return;
        }
        
        if (newPassword !== confirmPassword) {
            ui.showNotification('New passwords do not match', 'error');
            return;
        }
        
        try {
            const response = await fetch(`${config.apiUrl}/api/users/${state.getUserId()}/password`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${state.getToken()}`
                },
                body: JSON.stringify({
                    currentPassword,
                    newPassword
                })
            });
            
            if (response.ok) {
                ui.showNotification('Password changed successfully', 'success');
                changePasswordForm.reset();
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to change password');
            }
        } catch (error) {
            ui.showNotification(error.message, 'error');
            console.error('Error changing password:', error);
        }
    }

    // Handle logout
    function handleLogout(event) {
        event.preventDefault();
        auth.logout();
        window.location.href = 'index.html';
    }
});