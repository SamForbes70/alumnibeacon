-- P6: Stripe billing fields on tenants
ALTER TABLE tenants ADD COLUMN subscription_status TEXT NOT NULL DEFAULT 'trialing';
ALTER TABLE tenants ADD COLUMN subscription_period_end TEXT;
ALTER TABLE tenants ADD COLUMN monthly_investigation_limit INTEGER NOT NULL DEFAULT 10;
ALTER TABLE tenants ADD COLUMN investigations_used_this_month INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tenants ADD COLUMN billing_cycle_anchor TEXT;

-- Update existing tenants with sensible defaults per plan
UPDATE tenants SET monthly_investigation_limit = 50  WHERE plan = 'STARTER';
UPDATE tenants SET monthly_investigation_limit = 250 WHERE plan = 'PROFESSIONAL';
UPDATE tenants SET monthly_investigation_limit = 9999 WHERE plan = 'ENTERPRISE';
