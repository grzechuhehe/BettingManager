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
            const response = await fetch(`${config.apiUrl}/api/user/profile`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${state.getToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load user profile');
            }

            const userData = await response.json();
            
            // Display user data
            usernameDisplay.textContent = userData.username;
            emailDisplay.textContent = userData.email;
            createdDateDisplay.textContent = new Date(userData.createdAt).toLocaleDateString();
            
            // Populate update form
            usernameInput.value = userData.username;
            emailInput.value = userData.email;
            
        } catch (error) {
            ui.showNotification(error.message, 'error');
            console.error('Error loading profile:', error);
        }
    }

    // Function to load user betting statistics
    async function loadUserStatistics() {
        try {
            const response = await fetch(`${config.apiUrl}/api/bets/statistics`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${state.getToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load betting statistics');
            }

            const statsData = await response.json();
            
            // Update statistics display
            totalBetsElement.textContent = statsData.totalBets || 0;
            winRateElement.textContent = `${(statsData.winRate || 0).toFixed(1)}%`;
            profitLossElement.textContent = `$${(statsData.profitLoss || 0).toFixed(2)}`;
            avgOddsElement.textContent = (statsData.averageOdds || 0).toFixed(2);
            
            // Set color for profit/loss
            if (statsData.profitLoss > 0) {
                profitLossElement.classList.add('positive');
                profitLossElement.classList.remove('negative');
            } else if (statsData.profitLoss < 0) {
                profitLossElement.classList.add('negative');
                profitLossElement.classList.remove('positive');
            }
            
        } catch (error) {
            console.error('Error loading statistics:', error);
            // Don't show notification for stats error to avoid disrupting the user experience
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
            const response = await fetch(`${config.apiUrl}/api/user/profile`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${state.getToken()}`
                },
                body: JSON.stringify(userData)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to update profile');
            }
            
            ui.showNotification('Profile updated successfully', 'success');
            loadUserProfile(); // Reload profile data
            
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
            const response = await fetch(`${config.apiUrl}/api/user/password`, {
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
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to change password');
            }
            
            ui.showNotification('Password changed successfully', 'success');
            changePasswordForm.reset();
            
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