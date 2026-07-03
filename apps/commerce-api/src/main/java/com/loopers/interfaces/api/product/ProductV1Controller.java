package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.ProductSortOption;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    @Override
    public ApiResponse<ProductV1Dto.ProductListResponse> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false, defaultValue = "LATEST") ProductSortOption sort,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        List<ProductInfo> infos = productFacade.getProductList(brandId, sort, page, size);
        return ApiResponse.success(ProductV1Dto.ProductListResponse.from(infos));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(@PathVariable Long productId) {
        ProductInfo info = productFacade.getProductDetail(productId);
        // 조회 "사실" 발행 → 조회 수 집계는 이벤트 파이프라인(Outbox→Kafka→streamer)에서 처리
        eventPublisher.publishEvent(new ProductViewedEvent(productId, null));
        return ApiResponse.success(ProductV1Dto.ProductDetailResponse.from(info));
    }
}
