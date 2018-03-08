BEGIN
   DECLARE @PageNumber INT
   DECLARE @PageSize INT
   DECLARE @total INT
           SET @PageNumber = @@pageNumber@@
           SET @PageSize = @@pageSize@@
           SET @total = (SELECT count(CurrencyCode) FROM Sales.Currency)
           if @PageSize = -1
              SET @PageSize = @total
           SELECT
               CONVERT(varchar(255), CurrencyCode) as CurrencyCode, CONVERT(varchar(255), Name) as Name, CONVERT(varchar(255), ModifiedDate) as ModifiedDate, @total as total_rows
           FROM
               Sales.Currency
           ORDER BY
               CurrencyCode OFFSET @PageSize * (@PageNumber - 1) ROWS
           FETCH NEXT
               @PageSize ROWS ONLY
END;