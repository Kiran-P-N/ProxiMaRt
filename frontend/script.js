// This ensures all the HTML is loaded before the script runs
document.addEventListener('DOMContentLoaded', function() {
    
    // --- Cart Count Update ---
    const cartCountDesktop = document.getElementById('cart-count-desktop');
    const cartCountMobile = document.getElementById('cart-count-mobile');
    
    function updateCartCount() {
        // Reads the cart from browser storage
        const cart = JSON.parse(localStorage.getItem('cart')) || [];
        // Calculates the total number of items
        const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

        // Updates the numbers on both desktop and mobile icons
        if (cartCountDesktop) cartCountDesktop.textContent = totalItems;
        if (cartCountMobile) cartCountMobile.textContent = totalItems;
    }

    // --- Mobile Menu Toggle ---
    const mobileMenuButton = document.getElementById('mobile-menu-button');
    const mobileMenu = document.getElementById('mobile-menu');

    if (mobileMenuButton && mobileMenu) {
        mobileMenuButton.addEventListener('click', function() {
            mobileMenu.classList.toggle('hidden');
        });
    }

    // --- Fetch and Display Shops ---
    const shopsContainer = document.getElementById('shops-container');
    
    async function fetchShops() {
        if (!shopsContainer) {
            return; // Exit if we are not on the homepage
        }

        try {
            const response = await fetch('http://localhost:8080/api/vendors/');
            
            if (!response.ok) {
                throw new Error(`Network response was not ok, status: ${response.status}`);
            }
            
            const shops = await response.json();
            displayShops(shops);

        } catch (error) {
            console.error('Failed to fetch shops:', error);
            shopsContainer.innerHTML = '<p class="text-red-500 col-span-full text-center">Could not load shops. Please make sure the backend server is running.</p>';
        }
    }

    function displayShops(shops) {
        shopsContainer.innerHTML = '';
        
        if (shops.length === 0) {
            shopsContainer.innerHTML = '<p class="text-gray-500 col-span-full text-center">No shops have been added yet.</p>';
            return;
        }

        shops.forEach(shop => {
            const imageUrl = `https://placehold.co/600x400/22c55e/ffffff?text=${encodeURIComponent(shop.name)}`;

            const shopCard = `
                <div class="bg-white rounded-lg shadow-lg overflow-hidden transform hover:scale-105 transition-transform duration-300">
                    <img src="${imageUrl}" class="w-full h-48 object-cover" alt="${shop.name}">
                    <div class="p-6 text-left">
                        <h5 class="text-2xl font-bold text-gray-800">${shop.name}</h5>
                        <p class="text-gray-600 mt-2"><strong>Location:</strong> ${shop.location}</p>
                        <p class="text-gray-600 mt-1"><strong>Specialties:</strong> ${shop.specialties}</p>
                        <a href="products.html?id=${shop.id}" class="inline-block mt-4 bg-green-600 text-white font-semibold py-2 px-4 rounded-full hover:bg-green-700">View Shop</a>
                    </div>
                </div>
            `;
            shopsContainer.innerHTML += shopCard;
        });
    }
    
    // --- Initial function calls when the page loads ---
    updateCartCount(); // Update the cart count as soon as the homepage loads
    fetchShops();      // Fetch the shops from the backend
});

