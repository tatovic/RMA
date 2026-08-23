const { Router } = require('express');

const validate = require('../../middleware/validate');
const requireAdmin = require('../../middleware/requireAdmin');
const { createCategorySchema, updateCategorySchema } = require('./categories.schema');
const categoriesController = require('./categories.controller');

const router = Router();

// BR-003 — čitanje je dostupno svakom prijavljenom korisniku, izmene samo administratoru.
router.get('/', categoriesController.list);
router.post('/', requireAdmin, validate(createCategorySchema), categoriesController.create);
router.put('/:id', requireAdmin, validate(updateCategorySchema), categoriesController.update);
router.delete('/:id', requireAdmin, categoriesController.remove);

module.exports = router;
