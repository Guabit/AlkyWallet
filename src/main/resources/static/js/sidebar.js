document.addEventListener('DOMContentLoaded', () => {
    const btnMenu = document.getElementById('btn-menu');
    const menuNav = document.getElementById('menu-nav');
    const menuSalir = document.getElementById('menu-salir');

    if (btnMenu && menuNav) {
        btnMenu.addEventListener('click', () => {
            menuNav.classList.toggle('hidden');
            if (menuSalir) {
                menuSalir.classList.toggle('hidden');
            }
        });
    }
});