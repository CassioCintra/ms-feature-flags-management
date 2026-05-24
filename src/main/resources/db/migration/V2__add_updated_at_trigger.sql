CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER feature_flags_set_updated_at
    BEFORE UPDATE ON feature_flags
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
