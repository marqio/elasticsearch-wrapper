# elasticsearch-wrapper
/**
 *
 *@author Jason峰 Wind
 *@Date 2015-10-01
 *@Company Baidu
 *
 */
 
简介
1、elasticsearch代理服务，类似于DB-proxy、JDBC-sharding
2、主要提供了集群压力限流、读写分离、异步请求、异常跟踪记录、负载均衡、热部署、ES集群压力监控等等功能
3、纯JAVA编写、多线程、低耦合、模块化、可插拔、异步非阻塞web server服务！

Introducton
1、elasticsearch proxy service,like db-proxy、JDBC-sharding
2、Main modules supported currently:parallel tasks throttle、read-write separation、async request、illegal operations logging and tracking、load balance、hot deployment、cluster monitor and warning and so on.
3、Web service which built in java with multiple threads(threadPool)、Low Coupling and nio concept. In addition，Also designed in modules which can be added and removed	neatly if needed.

