document.addEventListener('DOMContentLoaded', () => {
    document.body.classList.add('page-loaded');

    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', event => {
            const message = form.getAttribute('data-confirm') || 'Are you sure?';
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    const anchors = document.querySelectorAll('a[href="#top"]');
    anchors.forEach(anchor => {
        anchor.addEventListener('click', event => {
            event.preventDefault();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    });
});
