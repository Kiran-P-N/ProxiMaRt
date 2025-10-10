document.addEventListener('DOMContentLoaded', function() {
    // --- DOM Elements ---
    const mobileMenuButton = document.getElementById('mobile-menu-button');
    const mobileMenu = document.getElementById('mobile-menu');
    const shopsContainer = document.getElementById('shops-container');
    const cartCountElement = document.getElementById('cart-count');
    const logoutBtnDesktop = document.getElementById('logout-btn-desktop');
    const logoutBtnMobile = document.getElementById('logout-btn-mobile');
    const searchInput = document.getElementById('search-input');
    const searchButton = document.getElementById('search-button');

    // --- Core Functions ---

    // Fetches shops from the backend based on a search term.
    // If no term is provided, it fetches all shops.
    async function performSearch() {
        const searchTerm = searchInput.value.trim();
        let url = 'http://localhost:8080/api/vendors/';

        // If there's a search term, add it to the URL as a query parameter.
        // The backend (VendorRoutes.java) will handle this.
        if (searchTerm) {
            url += `?q=${encodeURIComponent(searchTerm)}`;
        }

        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const shops = await response.json();
            displayShops(shops);
        } catch (error) {
            console.error('Failed to fetch shops:', error);
            if (shopsContainer) {
                shopsContainer.innerHTML = '<p class="col-span-full text-red-500">Could not load shops. Please try again later.</p>';
            }
        }
    }

    // Displays a given list of shops on the page
    function displayShops(shops) {
        if (!shopsContainer) return;
        shopsContainer.innerHTML = ''; // Clear existing content

        if (shops.length === 0) {
            shopsContainer.innerHTML = '<p class="col-span-full text-gray-500">No shops found matching your search.</p>';
            return;
        }

        shops.forEach(shop => {
            // Using a generic shop image for now. You could add an 'image_url' to your vendors table later.
            const shopImage = `images/shop${(shop.id % 4) + 1}.jpg`; 

            const shopCard = `
                <div class="bg-white rounded-lg shadow-lg overflow-hidden transform hover:scale-105 transition-transform duration-300 flex flex-col">
                    <img src="${shopImage}" class="w-full h-48 object-cover" alt="${shop.name}">
                    <div class="p-6 text-left flex flex-col flex-grow">
                        <h5 class="text-xl font-bold">${shop.name}</h5>
                        <p class="text-gray-600 mt-2">${shop.location}</p>
                        <div class="mt-auto pt-4">
                             <a href="products.html?id=${shop.id}" class="inline-block bg-green-600 text-white font-semibold py-2 px-4 rounded-full hover:bg-green-700">View Shop</a>
                        </div>
                    </div>
                </div>
            `;
            shopsContainer.innerHTML += shopCard;
        });
    }

    // --- Navbar and Cart Functions ---
    function handleLogout(e) {
        e.preventDefault();
        localStorage.removeItem('currentUser');
        localStorage.removeItem('cart');
        window.location.href = 'login.html';
    }

    function updateCartCount() {
        const cart = JSON.parse(localStorage.getItem('cart')) || [];
        const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
        if (cartCountElement) {
            cartCountElement.textContent = totalItems;
        }
    }

    // --- Event Listeners ---
    if (mobileMenuButton) {
        mobileMenuButton.addEventListener('click', () => mobileMenu.classList.toggle('hidden'));
    }
    if (logoutBtnDesktop) logoutBtnDesktop.addEventListener('click', handleLogout);
    if (logoutBtnMobile) logoutBtnMobile.addEventListener('click', handleLogout);

    window.addEventListener('pageshow', updateCartCount);

    // Search Event Listeners
    if (searchInput) {
        // Search as the user types (with a small delay to avoid too many requests)
        let searchTimeout;
        searchInput.addEventListener('keyup', () => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(performSearch, 300); // 300ms delay to prevent spamming the server
        });
    }
    if (searchButton) {
        searchButton.addEventListener('click', performSearch);
    }

    // --- Initial Load ---
    performSearch(); // Fetch all shops on initial page load
    updateCartCount();
});

