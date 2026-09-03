const { test, after, describe } = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');

const {
  api,
  pool,
  uuid,
  uniqueEmail,
  registerUser,
  loginAdmin,
  authHeader,
  seededUser,
  closePool,
} = require('./helpers');
const env = require('../src/config/env');

after(closePool);

// Granica autentifikacije — koje sve tokene server sme da prihvati i koje mora da odbije.

describe('Odsutan i neispravan token', () => {
  test('zahtev bez Authorization zaglavlja vraća 401 TOKEN_INVALID', async () => {
    const response = await api().get('/api/items').expect(401);
    assert.equal(response.body.error.code, 'TOKEN_INVALID');
  });

  test('pogrešna šema (Basic umesto Bearer) vraća 401 TOKEN_INVALID', async () => {
    const response = await api().get('/api/items').set({ Authorization: 'Basic abcdef' }).expect(401);
    assert.equal(response.body.error.code, 'TOKEN_INVALID');
  });

  test('token koji nije JWT vraća 401 TOKEN_INVALID', async () => {
    const response = await api().get('/api/items').set(authHeader('ne-postoji.token')).expect(401);
    assert.equal(response.body.error.code, 'TOKEN_INVALID');
  });

  test('token potpisan drugim ključem vraća 401 TOKEN_INVALID', async () => {
    const forged = jwt.sign({ sub: uuid(), role: 'ADMIN' }, 'a'.repeat(40), { expiresIn: '1h' });
    const response = await api().get('/api/items').set(authHeader(forged)).expect(401);
    assert.equal(response.body.error.code, 'TOKEN_INVALID');
  });

  test('istekao token vraća 401 TOKEN_EXPIRED, razdvojeno od nevažećeg', async () => {
    const user = await seededUser('auth-expired');
    const expired = jwt.sign({ sub: user.user.id, role: 'USER', tv: 0 }, env.JWT_SECRET, {
      expiresIn: '-1s',
    });

    const response = await api().get('/api/items').set(authHeader(expired)).expect(401);
    assert.equal(response.body.error.code, 'TOKEN_EXPIRED');
  });

  test('token korisnika koji više ne postoji vraća 401 TOKEN_INVALID', async () => {
    const user = await seededUser('auth-ghost');
    await pool.query('DELETE FROM users WHERE id = ?', [user.user.id]);

    const response = await api().get('/api/items').set(authHeader(user.token)).expect(401);
    assert.equal(response.body.error.code, 'TOKEN_INVALID');
  });
});

describe('FR-014 — nalog se proverava u bazi pri svakom zahtevu', () => {
  test('deaktiviran nalog je odbijen i sa još važećim tokenom', async () => {
    const user = await seededUser('auth-deactivated');

    // Token je izdat dok je nalog bio aktivan i nije istekao — jedina odbrana je provera u bazi.
    await api().get('/api/users/me').set(authHeader(user.token)).expect(200);

    const admin = await loginAdmin();
    await api()
      .patch(`/api/admin/users/${user.user.id}/status`)
      .set(authHeader(admin.token))
      .send({ isActive: false })
      .expect(200);

    const response = await api().get('/api/users/me').set(authHeader(user.token)).expect(403);
    assert.equal(response.body.error.code, 'ACCOUNT_DEACTIVATED');
  });

  test('deaktiviran nalog ne može ni da se prijavi', async () => {
    const user = await seededUser('auth-deactivated-login');

    const admin = await loginAdmin();
    await api()
      .patch(`/api/admin/users/${user.user.id}/status`)
      .set(authHeader(admin.token))
      .send({ isActive: false })
      .expect(200);

    const response = await api()
      .post('/api/auth/login')
      .send({ email: user.email, password: user.password })
      .expect(403);
    assert.equal(response.body.error.code, 'ACCOUNT_DEACTIVATED');
  });
});

describe('Prijava i registracija', () => {
  test('pogrešna lozinka i nepostojeći email daju identičan odgovor (FR-017)', async () => {
    const user = await seededUser('auth-same');

    const wrongPassword = await api()
      .post('/api/auth/login')
      .send({ email: user.email, password: 'PogresnaLozinka1' })
      .expect(401);

    const unknownEmail = await api()
      .post('/api/auth/login')
      .send({ email: uniqueEmail('nepostojeci'), password: 'PogresnaLozinka1' })
      .expect(401);

    assert.equal(wrongPassword.body.error.code, 'INVALID_CREDENTIALS');
    assert.deepEqual(unknownEmail.body, wrongPassword.body);
  });

  test('registracija sa zauzetim emailom vraća 409', async () => {
    const email = uniqueEmail('auth-dup');
    await registerUser({ email });

    const response = await api()
      .post('/api/auth/register')
      .send({ name: 'Dupli', email, password: 'Lozinka123', confirmPassword: 'Lozinka123' })
      .expect(409);

    assert.equal(response.body.error.code, 'EMAIL_ALREADY_EXISTS');
  });

  test('registracija posle prvog korisnika dodeljuje rolu USER (BR-001)', async () => {
    const user = await registerUser({ email: uniqueEmail('auth-role') });
    assert.equal(user.user.role, 'USER');
  });

  test('nova registracija dobija devet podrazumevanih lokacija (BR-015)', async () => {
    const user = await registerUser({ email: uniqueEmail('auth-locations') });
    const response = await api().get('/api/locations').set(authHeader(user.token)).expect(200);
    assert.equal(response.body.locations.length, 9);
  });
});

describe('Promena lozinke poništava ranije izdate tokene (nalaz 12)', () => {
  test('stari token prestaje da važi čim se lozinka promeni', async () => {
    const user = await seededUser('auth-token-version');
    const oldToken = user.token;

    await api().get('/api/users/me').set(authHeader(oldToken)).expect(200);

    await api()
      .post('/api/users/me/password')
      .set(authHeader(oldToken))
      .send({ currentPassword: user.password, newPassword: 'NovaLozinka123' })
      .expect(204);

    const response = await api().get('/api/users/me').set(authHeader(oldToken)).expect(401);
    assert.equal(response.body.error.code, 'TOKEN_INVALID');

    // Nova prijava daje token sa uvećanim `tv` claim-om i ponovo radi.
    const fresh = await api()
      .post('/api/auth/login')
      .send({ email: user.email, password: 'NovaLozinka123' })
      .expect(200);
    await api().get('/api/users/me').set(authHeader(fresh.body.token)).expect(200);
  });

  test('pogrešna trenutna lozinka ne menja ništa niti poništava token', async () => {
    const user = await seededUser('auth-wrong-current');

    const response = await api()
      .post('/api/users/me/password')
      .set(authHeader(user.token))
      .send({ currentPassword: 'NijeTacna123', newPassword: 'NovaLozinka123' })
      .expect(400);
    assert.equal(response.body.error.code, 'WRONG_CURRENT_PASSWORD');

    await api().get('/api/users/me').set(authHeader(user.token)).expect(200);
  });
});
