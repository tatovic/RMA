const { Router } = require('express');

const validate = require('../../middleware/validate');
const { createLocationSchema, updateLocationSchema } = require('./locations.schema');
const locationsController = require('./locations.controller');

const router = Router();

router.get('/', locationsController.list);
router.post('/', validate(createLocationSchema), locationsController.create);
router.put('/:id', validate(updateLocationSchema), locationsController.update);
router.delete('/:id', locationsController.remove);

module.exports = router;
