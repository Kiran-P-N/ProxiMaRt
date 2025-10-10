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
    async function performSearch() {
        const searchTerm = searchInput.value.trim();
        let url = 'http://localhost:8080/api/vendors/';

        if (searchTerm) {
            url += `?q=${encodeURIComponent(searchTerm)}`;
        }

        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error('Network response was not ok');
            const shops = await response.json();
            displayShops(shops);
        } catch (error) {
            console.error('Failed to fetch shops:', error);
            if (shopsContainer) {
                shopsContainer.innerHTML = '<p class="col-span-full text-red-500">Could not load shops.</p>';
            }
        }
    }

    function displayShops(shops) {
        if (!shopsContainer) return;
        shopsContainer.innerHTML = '';

        if (shops.length === 0) {
            shopsContainer.innerHTML = '<p class="col-span-full text-gray-500">No shops found.</p>';
            return;
        }

        shops.forEach(shop => {
            // --- UPDATED IMAGE LOGIC ---
            // Use the image from the database. If it's missing (null or empty), use a placeholder.
            const shopImage = shop.imageUrl ? shop.imageUrl : `https://placehold.co/600x400/a7f3d0/14532d?text=${encodeURIComponent(shop.name)}`;

            const shopCard = `
                <div class="bg-white rounded-lg shadow-lg overflow-hidden transform hover:scale-105 transition-transform duration-300 flex flex-col">
                    <img src="${shopImage}" class="w-full h-48 object-cover" alt="${shop.name}" onerror="this.onerror=null;this.src='https://placehold.co/600x400/eab308/ffffff?text=Image+Not+Found';">
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
    if (mobileMenuButton) mobileMenuButton.addEventListener('click', () => mobileMenu.classList.toggle('hidden'));
    if (logoutBtnDesktop) logoutBtnDesktop.addEventListener('click', handleLogout);
    if (logoutBtnMobile) logoutBtnMobile.addEventListener('click', handleLogout);
    window.addEventListener('pageshow', updateCartCount);

    if (searchInput) {
        let searchTimeout;
        searchInput.addEventListener('keyup', () => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(performSearch, 300);
        });
    }
    if (searchButton) searchButton.addEventListener('click', performSearch);

    // --- Initial Load ---
    performSearch();
    updateCartCount();
});

