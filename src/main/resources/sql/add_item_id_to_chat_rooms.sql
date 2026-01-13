-- Add ITEM_ID column to CHAT_ROOMS table to support item-based chat rooms
ALTER TABLE CHAT_ROOMS ADD ITEM_ID NUMBER(19);
COMMENT ON COLUMN CHAT_ROOMS.ITEM_ID IS 'Related Market Item ID';

-- Optional: Add Foreign Key if MARKET table exists (assuming table name is MARKET_ITEMS or similar, adjust if needed)
-- ALTER TABLE CHAT_ROOMS ADD CONSTRAINT FK_CHAT_ROOMS_ITEM FOREIGN KEY (ITEM_ID) REFERENCES MARKET_ITEMS(ID) ON DELETE SET NULL;
