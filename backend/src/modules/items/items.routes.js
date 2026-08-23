const { Router } = require('express');

const validate = require('../../middleware/validate');
const validateQuery = require('../../middleware/validateQuery');
const { createItemSchema, updateItemSchema, listItemsQuerySchema } = require('./items.schema');
const itemsController = require('./items.controller');

const router = Router();

router.get('/', validateQuery(listItemsQuerySchema), itemsController.list);
router.post('/', validate(createItemSchema), itemsController.create);
router.get('/:id', itemsController.getOne);
router.put('/:id', validate(updateItemSchema), itemsController.update);
router.delete('/:id', itemsController.remove);

module.exports = router;
