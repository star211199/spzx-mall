package com.spzx.product.service.impl;

import com.spzx.product.api.domain.Brand;
import com.spzx.product.mapper.BrandMapper;
import com.spzx.product.service.IBrandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BrandServiceImpl implements IBrandService {

    @Autowired
    BrandMapper brandMapper;

    @Override
    public List<Brand> list(Brand brand) {
        return brandMapper.list(brand);
    }

    @Override
    public Brand getById(Long id) {
        return brandMapper.getById(id);
    }

    @Override
    public int save(Brand brand) {
        int rows = brandMapper.insert(brand);
        return rows;
    }


    @Override
    public int update(Brand brand) {
        return brandMapper.update(brand);
    }

    @Override
    public int deleteBatch(Long[] ids) {
        return brandMapper.deleteBatch(ids);
    }

    @Override
    public List<Brand> selectBrandAll() {
        return brandMapper.selectBrandAll();
    }
}
