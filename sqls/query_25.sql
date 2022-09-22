select  *
from (select avg(ss_list_price) B1_LP
            ,count(ss_list_price) B1_CNT
            ,count(distinct ss_list_price) B1_CNTD
      from store_sales
      where ss_quantity between 0 and 5
        and (ss_list_price between 172 and 172+10 
             or ss_coupon_amt between 16859 and 16859+1000
             or ss_wholesale_cost between 7 and 7+20)) B1,
     (select avg(ss_list_price) B2_LP
            ,count(ss_list_price) B2_CNT
            ,count(distinct ss_list_price) B2_CNTD
      from store_sales
      where ss_quantity between 6 and 10
        and (ss_list_price between 137 and 137+10
          or ss_coupon_amt between 6156 and 6156+1000
          or ss_wholesale_cost between 9 and 9+20)) B2,
     (select avg(ss_list_price) B3_LP
            ,count(ss_list_price) B3_CNT
            ,count(distinct ss_list_price) B3_CNTD
      from store_sales
      where ss_quantity between 11 and 15
        and (ss_list_price between 3 and 3+10
          or ss_coupon_amt between 764 and 764+1000
          or ss_wholesale_cost between 8 and 8+20)) B3,
     (select avg(ss_list_price) B4_LP
            ,count(ss_list_price) B4_CNT
            ,count(distinct ss_list_price) B4_CNTD
      from store_sales
      where ss_quantity between 16 and 20
        and (ss_list_price between 43 and 43+10
          or ss_coupon_amt between 5670 and 5670+1000
          or ss_wholesale_cost between 64 and 64+20)) B4,
     (select avg(ss_list_price) B5_LP
            ,count(ss_list_price) B5_CNT
            ,count(distinct ss_list_price) B5_CNTD
      from store_sales
      where ss_quantity between 21 and 25
        and (ss_list_price between 133 and 133+10
          or ss_coupon_amt between 177 and 177+1000
          or ss_wholesale_cost between 17 and 17+20)) B5,
     (select avg(ss_list_price) B6_LP
            ,count(ss_list_price) B6_CNT
            ,count(distinct ss_list_price) B6_CNTD
      from store_sales
      where ss_quantity between 26 and 30
        and (ss_list_price between 118 and 118+10
          or ss_coupon_amt between 17512 and 17512+1000
          or ss_wholesale_cost between 3 and 3+20)) B6
limit 100;