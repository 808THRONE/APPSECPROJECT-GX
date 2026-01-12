export function register() {
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            const swUrl = '/service-worker.js';
            navigator.serviceWorker
                .register(swUrl)
                .then(registration => {
                    console.log('SW registered: ', registration);
                })
                .catch(error => {
                    console.log('SW registration failed: ', error);
                });
        });
    }
}

export function unregister() {
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.ready
            .then(registration => {
                registration.unregister();
            })
            .catch(error => {
                console.error(error.message);
            });
    }
}
