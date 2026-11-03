const header = document.querySelector('[data-header]');
const menuButton = document.querySelector('[data-menu-toggle]');
const nav = document.querySelector('[data-nav]');

const updateHeader = () => header?.classList.toggle('scrolled', window.scrollY > 20);
updateHeader();
window.addEventListener('scroll', updateHeader, { passive: true });

menuButton?.addEventListener('click', () => {
  const open = menuButton.getAttribute('aria-expanded') !== 'true';
  menuButton.setAttribute('aria-expanded', String(open));
  nav.classList.toggle('open', open);
});

nav?.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
  nav.classList.remove('open');
  menuButton?.setAttribute('aria-expanded', 'false');
}));

const revealObserver = new IntersectionObserver(entries => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
      revealObserver.unobserve(entry.target);
    }
  });
}, { threshold: 0.08, rootMargin: '0px 0px -35px' });

document.querySelectorAll('.reveal').forEach(element => revealObserver.observe(element));

const routes = {
  magisk: {
    tag: 'DOCUMENTED ROUTE',
    title: 'Magisk image patching',
    summary: 'Patch the correct image from the exact installed firmware, then follow the device maker’s supported flashing workflow.',
    requirements: ['Official OEM-unlock eligibility', 'Exact matching stock firmware', 'Computer and Android platform-tools', 'Verified recovery plan and complete backup'],
    warning: 'Never use another device’s patched image. Do not guess the target partition from Android version, and never relock with modified images installed.'
  },
  samsung: {
    tag: 'VENDOR-SPECIFIC',
    title: 'Samsung firmware route',
    summary: 'Samsung devices use Download Mode and a firmware-package workflow rather than generic fastboot instructions.',
    requirements: ['An officially unlockable exact variant', 'Matching model, region, and firmware', 'Understanding of permanent Knox effects', 'Current Samsung-specific Magisk guide'],
    warning: 'Carrier variants may be ineligible. Trill Root does not bypass KnoxGuard, FRP, carrier policy, or device management. Stop when OEM unlocking is unavailable.'
  },
  kernelsu: {
    tag: 'ADVANCED ROUTE',
    title: 'KernelSU compatibility review',
    summary: 'A kernel-level option only where the project explicitly supports the exact device, kernel, and installation mode.',
    requirements: ['Explicit device and kernel support', 'Exact stock boot images', 'Officially unlocked bootloader', 'Failed-boot recovery experience'],
    warning: 'Android version and a GKI label do not establish compatibility. An incompatible image can prevent boot; do not flash a generic kernel.'
  },
  legacy: {
    tag: 'DEPRECATED ROUTE',
    title: 'Legacy recovery documentation',
    summary: 'Some older devices retain a maintained model-specific recovery path, but it is not a universal fallback.',
    requirements: ['Exact device codename', 'Maintained model-specific recovery', 'Matching official firmware', 'Documented restore procedure'],
    warning: 'Custom-recovery installation is deprecated by modern Magisk guidance. Old packages and one-click tools can be unmaintained or unsafe.'
  }
};

const routeButtons = document.querySelectorAll('[data-route]');
const routeTag = document.querySelector('[data-route-tag]');
const routeTitle = document.querySelector('[data-route-title]');
const routeSummary = document.querySelector('[data-route-summary]');
const routeList = document.querySelector('[data-route-list]');
const routeWarning = document.querySelector('[data-route-warning]');

routeButtons.forEach(button => button.addEventListener('click', () => {
  const route = routes[button.dataset.route];
  if (!route) return;
  routeButtons.forEach(other => other.setAttribute('aria-selected', String(other === button)));
  routeTag.textContent = route.tag;
  routeTitle.textContent = route.title;
  routeSummary.textContent = route.summary;
  routeWarning.textContent = route.warning;
  routeList.replaceChildren(...route.requirements.map(item => {
    const li = document.createElement('li');
    li.textContent = item;
    return li;
  }));
}));
