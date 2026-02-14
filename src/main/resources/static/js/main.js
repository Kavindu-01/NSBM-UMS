document.addEventListener('DOMContentLoaded', () => {
    const root = document.body;
    root.classList.add('page-loaded');

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const allowAmbient = document.documentElement.dataset.fx === 'on';

    const mountBackdrop = () => {
        if (document.querySelector('.aura-grid')) {
            return;
        }
        const aura = document.createElement('div');
        aura.className = 'aura-grid';
        document.body.appendChild(aura);

        ['primary', 'secondary'].forEach((variant, index) => {
            const orb = document.createElement('div');
            orb.className = `liquid-orb ${variant === 'secondary' ? 'orb-secondary' : ''}`.trim();
            orb.style.top = `${15 + index * 20}%`;
            orb.style.left = `${index === 0 ? 10 : 75}%`;
            document.body.appendChild(orb);
        });
    };

    if (allowAmbient && !prefersReducedMotion) {
        requestAnimationFrame(mountBackdrop);
    }

    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', event => {
            const message = form.getAttribute('data-confirm') || 'Are you sure?';
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll('a[href="#top"]').forEach(anchor => {
        anchor.addEventListener('click', event => {
            event.preventDefault();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    });

    document.querySelectorAll('[data-reveal]').forEach(el => el.classList.add('is-visible'));

    const dropdownToggles = document.querySelectorAll('.nav-glass .dropdown-toggle');
    dropdownToggles.forEach(toggle => {
        toggle.addEventListener('shown.bs.dropdown', event => {
            event.target.classList.add('active');
        });
        toggle.addEventListener('hidden.bs.dropdown', event => {
            event.target.classList.remove('active');
        });
    });

    const facultySelect = document.querySelector('[data-semester-faculty-select]');
    const degreeSelect = document.querySelector('[data-semester-degree-select]');
    if (facultySelect && degreeSelect) {
        const baseDegreeOptions = Array.from(degreeSelect.options).map(option => option.cloneNode(true));
        const renderDegrees = () => {
            const selectedFaculty = facultySelect.value;
            degreeSelect.innerHTML = '';
            const fragment = document.createDocumentFragment();
            baseDegreeOptions.forEach(optionTemplate => {
                const optionFaculty = optionTemplate.dataset.facultyId;
                if (!selectedFaculty || !optionFaculty || optionFaculty === selectedFaculty) {
                    fragment.appendChild(optionTemplate.cloneNode(true));
                }
            });
            degreeSelect.appendChild(fragment);
            degreeSelect.selectedIndex = 0;
        };
        facultySelect.addEventListener('change', renderDegrees);
        renderDegrees();
    }

    const initPasswordMeters = () => {
        const inputs = document.querySelectorAll('[data-password-meter]');
        if (!inputs.length) {
            return;
        }

        const evaluateStrength = value => {
            if (!value) {
                return { score: 0, label: 'Start typing', state: 'idle' };
            }
            let score = 0;
            const lengthBonus = Math.min(3, Math.floor(value.length / 4));
            score += lengthBonus;

            const checks = [
                /[a-z]/.test(value),
                /[A-Z]/.test(value),
                /[0-9]/.test(value),
                /[^A-Za-z0-9]/.test(value)
            ];
            score += checks.filter(Boolean).length;

            if (!/(.)\1{2,}/.test(value)) {
                score += 1;
            }

            if (new Set(value).size >= 8) {
                score += 1;
            }

            const normalized = Math.min(score / 8, 1);
            if (normalized >= 0.8) {
                return { score: normalized, label: 'Excellent', state: 'strong' };
            }
            if (normalized >= 0.6) {
                return { score: normalized, label: 'Good', state: 'good' };
            }
            if (normalized >= 0.4) {
                return { score: normalized, label: 'Fair', state: 'fair' };
            }
            return { score: normalized, label: 'Too weak', state: 'weak' };
        };

        inputs.forEach((input, index) => {
            if (input.dataset.meterAttached === 'true') {
                return;
            }
            input.dataset.meterAttached = 'true';
            const descriptor = input.dataset.passwordMeter || 'Password strength';
            const wrapper = document.createElement('div');
            wrapper.className = 'password-meter';
            wrapper.dataset.strengthState = 'idle';

            const track = document.createElement('div');
            track.className = 'password-meter-track';
            const fill = document.createElement('div');
            fill.className = 'password-meter-fill';
            track.appendChild(fill);

            const status = document.createElement('p');
            status.className = 'password-meter-status';
            const meterId = input.id ? `${input.id}-meter` : `password-meter-${index}`;
            status.id = meterId;
            status.setAttribute('aria-live', 'polite');
            status.textContent = `${descriptor}: Start typing`;

            wrapper.appendChild(track);
            wrapper.appendChild(status);
            input.insertAdjacentElement('afterend', wrapper);

            const ariaDescribedBy = input.getAttribute('aria-describedby');
            input.setAttribute('aria-describedby', ariaDescribedBy ? `${ariaDescribedBy} ${meterId}` : meterId);

            const updateMeter = () => {
                const { score, label, state } = evaluateStrength(input.value);
                const clamped = Math.max(score, 0.08);
                fill.style.transform = `scaleX(${clamped})`;
                wrapper.dataset.strengthState = input.value ? state : 'idle';
                status.textContent = `${descriptor}: ${label}`;
                if (input.value || document.activeElement === input) {
                    wrapper.classList.add('is-visible');
                } else {
                    wrapper.classList.remove('is-visible');
                }
            };

            input.addEventListener('input', updateMeter);
            input.addEventListener('focus', updateMeter);
            input.addEventListener('blur', updateMeter);
            updateMeter();
        });
    };

    initPasswordMeters();
});
