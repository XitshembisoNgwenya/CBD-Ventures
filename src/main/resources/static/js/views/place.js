import { api } from '../api.js?v=20261112-2';
import { icon, escapeHtml, distanceText } from '../ui.js?v=20261112-2';
import { addBusinessProfiles } from './place-profiles.js?v=20261112-2';

export async function renderPlace(container, id) {
  if (!id) { location.hash = '#/explore'; return; }

  container.innerHTML = '<div class="skeleton" style="height:300px;margin-top:8px"></div>';

  let place;
  try {
    const result = await api.assistantPlace(id);
    place = result.data;
  } catch (error) {
    container.innerHTML = '<div class="card empty" style="margin-top:26px">' +
      '<span class="empty__icon">' + icon('alert', 22) + '</span>' +
      '<strong>That place is not available</strong>' +
      '<p class="muted small">' + escapeHtml(error.message) + '</p>' +
      '<a class="btn btn--sm" href="#/explore">Back to Explore</a></div>';
    return;
  }

  const [matched] = await addBusinessProfiles([place]);
  place = matched;
  if (!place.withinRadius) {
    container.innerHTML = '<div class="card empty" style="margin-top:26px">' +
      '<span class="empty__icon">' + icon('pin', 22) + '</span>' +
      '<strong>This place is outside the 1 km guide</strong>' +
      '<p class="muted small">CBDVentures only recommends places with a map pin inside the Richfield campus radius.</p>' +
      '<a class="btn btn--sm" href="#/explore">Browse nearby places</a></div>';
    return;
  }

  const profile = place.businessProfile || null;
  const phone = profile?.phone || extractPhone(place.description);
  const menuItems = place.menuItems || [];
  const distance = place.distanceMeters === null || place.distanceMeters === undefined
    ? 'Distance not confirmed' : (/approx|pin supplied|exact street address not provided/i.test(place.verificationNote || '') ? '≈ ' : '') + distanceText(Number(place.distanceMeters) / 1000);
  const hero = place.image || profile?.thumbnail || '/images/hero-welcome.svg';
  const rating = profile?.reviewCount
    ? icon('star', 13) + ' ' + Number(profile.rating || 0).toFixed(1) + ' (' + profile.reviewCount + ' reviews)'
    : 'No published ratings';
  const profileLink = profile
    ? '<a class="btn btn--ghost" href="#/business/' + encodeURIComponent(profile.id) + '">Full profile &amp; reviews</a>'
    : '';

  container.innerHTML =
    '<div class="curated-place__hero">' +
      '<img src="' + escapeHtml(hero) + '" alt="' + escapeHtml(place.name) + '">' +
      '<a class="biz-hero__back" href="#/explore" aria-label="Back to Explore">' + icon('arrowLeft', 20) + '</a>' +
    '</div>' +
    '<div class="biz-head">' +
      '<span class="badge badge--violet">' + escapeHtml(place.categoryLabel || 'Place') + '</span>' +
      '<h1>' + escapeHtml(place.name) + '</h1>' +
      '<div class="biz-head__meta">' +
        '<span>' + icon('pin', 13) + ' ' + escapeHtml(distance) + '</span>' +
        '<span class="rating-inline">' + rating + '</span>' +
      '</div>' +
    '</div>' +
    '<div class="biz-actions">' +
      '<a class="btn" href="' + escapeHtml(place.directionsUrl) + '" target="_blank" rel="noopener noreferrer">' +
        icon('directions', 17) + ' Get walking directions</a>' +
      (phone ? '<a class="btn btn--ghost" href="tel:' + escapeHtml(phone.replace(/[^\d+]/g, '')) +
        '" aria-label="Call ' + escapeHtml(place.name) + '">' + icon('phone', 17) + '</a>' : '') +
      profileLink +
    '</div>' +
    (menuItems.length
      ? '<section class="place-menu-section">' +
          '<div class="section-head"><h2>What’s on the menu</h2><span class="muted small">' +
            menuItems.length + ' item' + (menuItems.length === 1 ? '' : 's') + '</span></div>' +
          '<div class="menu-grid">' + menuItems.map((item) => menuTile(item, place)).join('') + '</div>' +
          (place.menuNote ? '<p class="field__hint place-menu-note">' + escapeHtml(place.menuNote) + '</p>' : '') +
          (place.menuSourceUrl
            ? '<a class="btn btn--ghost btn--sm" href="' + escapeHtml(place.menuSourceUrl) +
                '" target="_blank" rel="noopener noreferrer">Open published menu</a>'
            : '') +
        '</section>'
      : place.category === 'food'
        ? '<div class="card place-menu-empty" style="margin-top:16px"><strong>Menu not published yet</strong>' +
            '<p class="muted small">This nearby place is listed, but its current items and prices have not been confirmed.</p></div>'
        : '') +
    '<div class="card place-facts" style="margin-top:16px">' +
      '<strong>Place details</strong>' +
      '<div class="fact-list" style="margin-top:8px">' +
        '<div class="fact"><span class="fact__icon">' + icon('pin', 16) + '</span>' +
          '<div><div class="fact__label">Address</div><div class="fact__value">' + escapeHtml(place.address) + '</div></div></div>' +
        '<div class="fact"><span class="fact__icon">' + icon('star', 16) + '</span>' +
          '<div><div class="fact__label">Ratings</div><div class="fact__value">' + escapeHtml(
            profile?.reviewCount ? Number(profile.rating || 0).toFixed(1) + ' from ' + profile.reviewCount + ' reviews'
              : 'No ratings have been published for this place yet.') + '</div></div></div>' +
        (phone
          ? '<div class="fact"><span class="fact__icon">' + icon('phone', 16) + '</span>' +
            '<div><div class="fact__label">Contact number</div><div class="fact__value"><a href="tel:' +
              escapeHtml(phone.replace(/[^\d+]/g, '')) + '">' + escapeHtml(phone) + '</a></div></div></div>'
          : '') +
        (profile?.ownerName
          ? '<div class="fact"><span class="fact__icon">' + icon('user', 16) + '</span>' +
            '<div><div class="fact__label">Managed by</div><div class="fact__value">' +
              escapeHtml(profile.ownerName) + '</div></div></div>'
          : '') +
      '</div>' +
    '</div>' +
    (place.verificationNote
      ? '<p class="field__hint place-verification-note">' + escapeHtml(place.verificationNote) + '</p>'
      : '') +
    '<a class="btn btn--ghost" style="margin-top:14px" href="#/explore">' +
      icon('arrowLeft', 15) + ' Back to Explore</a>';

  container.querySelectorAll('.menu-tile__photo').forEach((img) => {
    img.addEventListener('error', () => img.classList.add('is-broken'), { once: true });
  });
}

function menuTile(item, place) {
  const hasPrice = item.price !== null && item.price !== undefined && Number.isFinite(Number(item.price));
  const price = hasPrice ? 'R' + Number(item.price).toFixed(2) : 'Check menu';
  const imageUrl = item.image || place.image;
  const imageAlt = item.image ? item.name : place.name + ' storefront';
  return '<article class="menu-tile">' +
    '<div class="menu-tile__media">' +
      '<span class="menu-tile__fallback">' + icon('food', 25) + '</span>' +
      (imageUrl ? '<img class="menu-tile__photo" src="' + escapeHtml(imageUrl) +
        '" alt="' + escapeHtml(imageAlt) + '" loading="lazy">' : '') +
      '<span class="menu-tile__price">' + escapeHtml(price) + '</span>' +
    '</div>' +
    '<div class="menu-tile__body"><strong>' + escapeHtml(item.name) + '</strong>' +
      '<span>' + escapeHtml(item.description || place.name) + '</span></div></article>';
}

function extractPhone(description = '') {
  const match = String(description).match(/(?:phone|telephone|tel)\s*:\s*([+()\d][+()\d\s.-]{6,})/i);
  return match?.[1]?.trim().replace(/[.,;]+$/, '') || '';
}
