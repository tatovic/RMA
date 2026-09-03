const { test, after, describe } = require('node:test');
const assert = require('node:assert/strict');

const {
  api,
  uuid,
  authHeader,
  itemPayload,
  createItem,
  seededUser,
  loginAdmin,
  closePool,
} = require('./helpers');

after(closePool);

// Pravila vlasništva OWN-01 do OWN-07 (db.md sekcija 9). Do tiketa 28 su živela samo u Postman
// kolekciji, koju je neko morao da se seti da pokrene — ovi testovi ih drže automatski.

describe('OWN-03 — tuđ predmet je nevidljiv, ne zabranjen', () => {
  test('GET tuđeg predmeta vraća 404, ne 403', async () => {
    const owner = await seededUser('own-a');
    const other = await seededUser('own-b');
    const item = await createItem(
      owner.token,
      itemPayload({ categoryId: owner.categoryId, locationId: owner.locationId })
    );

    const response = await api().get(`/api/items/${item.id}`).set(authHeader(other.token)).expect(404);
    assert.equal(response.body.error.code, 'NOT_FOUND');
  });

  test('PUT tuđeg predmeta vraća 404 i ne menja red', async () => {
    const owner = await seededUser('own-c');
    const other = await seededUser('own-d');
    const item = await createItem(
      owner.token,
      itemPayload({ categoryId: owner.categoryId, locationId: owner.locationId, name: 'Original' })
    );

    await api()
      .put(`/api/items/${item.id}`)
      .set(authHeader(other.token))
      .send({
        ...itemPayload({
          id: item.id,
          categoryId: other.categoryId,
          locationId: other.locationId,
          name: 'Otet',
        }),
        updatedAt: item.updatedAt,
      })
      .expect(404);

    const stillOriginal = await api().get(`/api/items/${item.id}`).set(authHeader(owner.token)).expect(200);
    assert.equal(stillOriginal.body.name, 'Original');
  });

  test('DELETE tuđeg predmeta vraća 404 i ne briše red', async () => {
    const owner = await seededUser('own-e');
    const other = await seededUser('own-f');
    const item = await createItem(
      owner.token,
      itemPayload({ categoryId: owner.categoryId, locationId: owner.locationId })
    );

    await api().delete(`/api/items/${item.id}`).set(authHeader(other.token)).expect(404);
    await api().get(`/api/items/${item.id}`).set(authHeader(owner.token)).expect(200);
  });

  test('lista predmeta nikad ne sadrži tuđe redove', async () => {
    const owner = await seededUser('own-g');
    const other = await seededUser('own-h');
    const item = await createItem(
      owner.token,
      itemPayload({ categoryId: owner.categoryId, locationId: owner.locationId })
    );

    const response = await api().get('/api/items').set(authHeader(other.token)).expect(200);
    assert.equal(
      response.body.items.some((row) => row.id === item.id),
      false
    );
  });
});

describe('OWN-02 — userId iz tela zahteva se ignoriše', () => {
  test('POST sa tuđim userId upisuje predmet vlasniku tokena', async () => {
    const owner = await seededUser('own-i');
    const other = await seededUser('own-j');

    const response = await api()
      .post('/api/items')
      .set(authHeader(owner.token))
      .send({
        ...itemPayload({ categoryId: owner.categoryId, locationId: owner.locationId }),
        userId: other.user.id,
      })
      .expect(201);

    assert.equal(response.body.userId, owner.user.id);
  });

  test('PUT sa tuđim userId ne prebacuje vlasništvo', async () => {
    const owner = await seededUser('own-k');
    const other = await seededUser('own-l');
    const item = await createItem(
      owner.token,
      itemPayload({ categoryId: owner.categoryId, locationId: owner.locationId })
    );

    const response = await api()
      .put(`/api/items/${item.id}`)
      .set(authHeader(owner.token))
      .send({
        ...itemPayload({ id: item.id, categoryId: owner.categoryId, locationId: owner.locationId }),
        userId: other.user.id,
        updatedAt: item.updatedAt,
      })
      .expect(200);

    assert.equal(response.body.userId, owner.user.id);
  });
});

describe('OWN-04 / DB-RULE-01 — lokacija mora pripadati istom korisniku', () => {
  test('POST predmeta sa tuđom lokacijom vraća 404', async () => {
    const owner = await seededUser('own-m');
    const other = await seededUser('own-n');

    const response = await api()
      .post('/api/items')
      .set(authHeader(owner.token))
      .send(itemPayload({ categoryId: owner.categoryId, locationId: other.locationId }))
      .expect(404);

    assert.equal(response.body.error.code, 'NOT_FOUND');
  });

  test('PUT predmeta na tuđu lokaciju vraća 404', async () => {
    const owner = await seededUser('own-o');
    const other = await seededUser('own-p');
    const item = await createItem(
      owner.token,
      itemPayload({ categoryId: owner.categoryId, locationId: owner.locationId })
    );

    await api()
      .put(`/api/items/${item.id}`)
      .set(authHeader(owner.token))
      .send({
        ...itemPayload({ id: item.id, categoryId: owner.categoryId, locationId: other.locationId }),
        updatedAt: item.updatedAt,
      })
      .expect(404);
  });

  test('DELETE tuđe lokacije vraća 404', async () => {
    const owner = await seededUser('own-q');
    const other = await seededUser('own-r');

    await api().delete(`/api/locations/${owner.locationId}`).set(authHeader(other.token)).expect(404);
  });
});

describe('OWN-06 — admin vidi brojače, nikad sadržaj tuđeg inventara', () => {
  test('lista korisnika izlaže itemCount i nijedno polje predmeta', async () => {
    const user = await seededUser('own-s');
    await createItem(
      user.token,
      itemPayload({
        categoryId: user.categoryId,
        locationId: user.locationId,
        name: 'Tajni predmet korisnika',
      })
    );

    const admin = await loginAdmin();
    const response = await api().get('/api/admin/users').set(authHeader(admin.token)).expect(200);

    const row = response.body.find((entry) => entry.id === user.user.id);
    assert.ok(row, 'korisnik mora biti na listi');
    assert.equal(row.itemCount, 1);

    const allowedFields = ['id', 'name', 'email', 'role', 'isActive', 'createdAt', 'itemCount'];
    assert.deepEqual(Object.keys(row).sort(), [...allowedFields].sort());
    assert.equal(JSON.stringify(response.body).includes('Tajni predmet korisnika'), false);
  });
});

describe('FR-106 / BR-003 — rola USER nema pristup administraciji', () => {
  test('sve tri administratorske rute vraćaju 403 za rolu USER', async () => {
    const user = await seededUser('own-t');

    for (const call of [
      () => api().get('/api/admin/stats').set(authHeader(user.token)),
      () => api().get('/api/admin/users').set(authHeader(user.token)),
      () =>
        api()
          .patch(`/api/admin/users/${user.user.id}/status`)
          .set(authHeader(user.token))
          .send({ isActive: false }),
    ]) {
      const response = await call().expect(403);
      assert.equal(response.body.error.code, 'FORBIDDEN');
    }
  });

  test('izmene kategorija su zabranjene roli USER, čitanje nije', async () => {
    const user = await seededUser('own-u');

    await api().get('/api/categories').set(authHeader(user.token)).expect(200);

    const created = await api()
      .post('/api/categories')
      .set(authHeader(user.token))
      .send({ name: `Nova ${uuid().slice(0, 8)}`, sortOrder: 99 })
      .expect(403);
    assert.equal(created.body.error.code, 'FORBIDDEN');

    await api()
      .put(`/api/categories/${uuid()}`)
      .set(authHeader(user.token))
      .send({ name: 'Preimenovana', sortOrder: 1 })
      .expect(403);

    await api().delete(`/api/categories/${uuid()}`).set(authHeader(user.token)).expect(403);
  });
});

describe('BR-004 — admin ne može da deaktivira sopstveni nalog', () => {
  test('samodeaktivacija vraća 409 CANNOT_DEACTIVATE_SELF', async () => {
    const admin = await loginAdmin();

    const response = await api()
      .patch(`/api/admin/users/${admin.user.id}/status`)
      .set(authHeader(admin.token))
      .send({ isActive: false })
      .expect(409);

    assert.equal(response.body.error.code, 'CANNOT_DEACTIVATE_SELF');

    const stillActive = await api().get('/api/users/me').set(authHeader(admin.token)).expect(200);
    assert.equal(stillActive.body.isActive, true);
  });
});
