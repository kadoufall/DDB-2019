package transaction.models;

import transaction.exceptions.InvalidIndexException;

import java.io.Serializable;

public class Customer implements ResourceItem, Serializable {
    private String custName;
    private boolean isdeleted;

    public Customer(String custName) {
        this.custName = custName;
        isdeleted = false;
    }

    public String getCustName() {
        return custName;
    }

    @Override
    public String[] getColumnNames() {
        return new String[0];
    }

    @Override
    public String[] getColumnValues() {
        return new String[0];
    }

    @Override
    public Object getIndex(String indexName) throws InvalidIndexException {
        return null;
    }

    @Override
    public Object getKey() {
        return this.custName;
    }

    @Override
    public boolean isDeleted() {
        return this.isdeleted;
    }

    @Override
    public void delete() {
        this.isdeleted = true;
    }

    @Override
    public Object clone() {
        return new Customer(getCustName());
    }
}
